package cn.cruder.dousx.dcredis.factory;


import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.component.DcredisAuxiliary;
import cn.cruder.dousx.dcredis.constant.TopicConstant;
import cn.cruder.tools.json.JsonUtilPool;
import io.vavr.Tuple2;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component(DcredisProxyFactory.BEAN_NAME)
public class DcredisProxyFactory {
    private static final Logger log = LoggerFactory.getLogger(DcredisProxyFactory.class);

    public static final String BEAN_NAME = "dcredisProxyFactory";
    private static final Map<String, Tuple2<Class<?>, Object>> LOCAL_CACHE = new ConcurrentHashMap<>();

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private DcredisAuxiliary dcredisAuxiliary;

    public <T> T createProxy(Class<T> configInterface) {
        Object proxy = Proxy.newProxyInstance(
                configInterface.getClassLoader(),
                new Class<?>[]{configInterface},
                new ConfigInvocationHandler()
        );
        return configInterface.cast(proxy);
    }

    private class ConfigInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            DcredisProperty dcredisProperty = method.getAnnotation(DcredisProperty.class);
            if (dcredisProperty == null) {
                throw new UnsupportedOperationException("Method not annotated with @DynamicConfigProperty");
            }
            String annotationKey = dcredisProperty.key();
            String redisKey = dcredisAuxiliary.getRedisConfigKey(annotationKey);
            Tuple2<Class<?>, Object> tuple2 = LOCAL_CACHE.get(redisKey);
            if (tuple2 == null) {
                RLock lock = dcredisAuxiliary.getDcredisLock();
                lock.lock();
                try {
                    tuple2 = LOCAL_CACHE.get(redisKey);
                    if (tuple2 == null) {
                        Class<?> returnType = method.getReturnType();
                        Object redisValue = redissonClient.getBucket(redisKey).get();
                        Object config = null;
                        if (redisValue != null) {
                            config = conversion(redisKey, redisValue, returnType);
                            log.info("取Redis配置：{} ==> {}", redisKey, JsonUtilPool.toJsonString(config));
                        } else {
                            String defV = dcredisProperty.defaultValue();
                            Object dv = JsonUtilPool.parseObject(defV, returnType);
                            redissonClient.getBucket(redisKey).set(dv);
                            config = dv;
                            // 这里有个细节要注意，redis的值是不能为空的，如果为空会取默认值，将默认值放到redis中。
                            // 所以如果redis中配置了值，出现redis宕机导致值没有了，此时可能会取默认值(可能是错误的值)
                            // 这里暂不讨论redis崩溃导致的配置丢失的问题,假设redis做了主从备份。配置不会丢失
                            log.info("Redis中不存在值，取默认配置：{} ==> {}", redisKey, defV);
                        }
                        if (config == null) {
                            throw new NullPointerException(String.format("[%s] config is null", annotationKey));
                        }
                        tuple2 = new Tuple2<>(returnType, config);
                        LOCAL_CACHE.put(redisKey, tuple2);
                    }
                } finally {
                    lock.unlock();
                }
            }
            return tuple2._2();
        }
    }

    @PostConstruct
    public void init() {
        // bean初始化以后再添加监听事件。(放到ConfigInvocationHandler构造方法中会重复添加监听事件)
        // 放到bean初始化以后添加，可能会造成服务启动后存在短暂的时间段监听事件还没添加上。不过这个时间很短，可以忽略
        log.info("DcredisProxyFactory init start");
        RTopic topic = redissonClient.getTopic(TopicConstant.CONFIG_TOPIC);
        topic.addListener(String.class, (channelTopic, redisKey) -> {
            Tuple2<Class<?>, Object> tup = LOCAL_CACHE.get(redisKey);
            if (tup == null) {
                log.warn("key is not exist:[{}] {}", channelTopic, redisKey);
                return;
            }

            RLock dcredisLock = dcredisAuxiliary.getDcredisLock();
            dcredisLock.lock();
            try {
                Object redisValue = redissonClient.getBucket(redisKey).get();
                if (redisValue == null) {
                    throw new NullPointerException(String.format("更新配置异常(redis中不存在当前配置)：[%s] [%s]", channelTopic, redisKey));
                }
                Class<?> returnType = tup._1();
                Object config = conversion(redisKey, redisValue, returnType);
                // 如果是复杂类型，这里要实现下toString()方法，否则会打印出内存地址
                log.info("update local Cache:[{}][{}]  {} ==> {}", channelTopic, redisKey, tup._2(), config);
                Tuple2<Class<?>, Object> value = new Tuple2<>(returnType, config);
                LOCAL_CACHE.put(redisKey, value);
            } finally {
                dcredisLock.unlock();
            }
        });
        log.info("DcredisProxyFactory init done");
    }

    /**
     * 转换
     * <br/>
     * 这里多做一步操作，将redis的map类型转成returnType类型
     * <br/>
     * 原因是cn.cruder.dousx.dcredis.component.DcredisConfigUpdater.updateRedisConfig是通用更新缓存方法
     * <br/>
     * 接收的缓存对象是Object，如果是接口调用，可能会传map类型，此时redis中存的是map
     * <p>
     * 这里处理的原因有以下几点考虑：
     * <li/>1. 可以在更新缓存方法中，对map类型进行转换，但是这样会暴露给用户，用户可能忘记转换，导致缓存更新失败。
     * <li/>2. 将转换逻辑统一起来，放到一块方便管理
     */
    private Object conversion(String redisKey, Object redisValue, Class<?> returnType) {
        if (redisValue instanceof Map<?, ?>) {
            Object config = JsonUtilPool.parseObject(JsonUtilPool.toJsonString(redisValue), returnType);
            redissonClient.getBucket(redisKey).set(config);
            log.info("类型转换[{}] {} ==> {}", redisKey, redisValue.getClass().getName(), returnType.getName());
            return config;
        }
        return redisValue;
    }
}
