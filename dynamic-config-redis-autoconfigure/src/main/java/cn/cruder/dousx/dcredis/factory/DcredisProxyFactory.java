package cn.cruder.dousx.dcredis.factory;


import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.component.DcredisKeyComponent;
import cn.cruder.dousx.dcredis.constant.TopicConstant;
import cn.cruder.tools.json.JsonUtilPool;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
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
    /**
     * 本地缓存
     */
    private static final Map<String, Object> LOCAL_CACHE = new ConcurrentHashMap<>();

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private DcredisKeyComponent dcredisKeyComponent;

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
            String redisKey = dcredisKeyComponent.getRedisKey(annotationKey);
            Object config = LOCAL_CACHE.get(redisKey);
            if (config == null) {
                synchronized (LOCAL_CACHE) {
                    config = LOCAL_CACHE.get(redisKey);
                    if (config == null) {
                        String configValue = "";
                        Object redisValue = redissonClient.getBucket(redisKey).get();
                        if (redisValue != null) {
                            configValue = (String) redisValue;
                        }
                        if (StringUtils.isBlank(configValue)) {
                            configValue = dcredisProperty.defaultValue();
                            log.debug("取默认配置：{} ==> {}", redisKey, configValue);
                        }
                        // 去除Json字符串的转义字符
                        String removeEscapeStr = JsonParser.parseString(configValue).toString();
                        log.info("Before:{} ===> After:{}", configValue, removeEscapeStr);
                        Class<?> returnType = method.getReturnType();
                        config = JsonUtilPool.parseObject(removeEscapeStr, returnType);
                        LOCAL_CACHE.put(redisKey, config);
                    }
                }
            }
            return config;
        }
    }

    @PostConstruct
    public void init() {
        // bean初始化以后再添加监听事件。(放到ConfigInvocationHandler构造方法中会重复添加监听事件)
        // 放到bean初始化以后添加，可能会造成服务启动后存在短暂的时间段监听事件还没添加上。不过这个时间很短，可以忽略
        log.info("DcredisProxyFactory init start");
        RTopic topic = redissonClient.getTopic(TopicConstant.CONFIG_TOPIC);
        topic.addListener(String.class, (channelTopic, redisKey) -> {
            String value = (String) redissonClient.getBucket(redisKey).get();
            if (LOCAL_CACHE.containsKey(redisKey)) {
                Class<?> returnType = LOCAL_CACHE.get(redisKey).getClass();
                String removeEscapeStr = JsonParser.parseString(value).toString();
                log.info("removeEscapeBefore:{} ===> removeEscapeAfter:{}", value, removeEscapeStr);
                Object config = JsonUtilPool.parseObject(removeEscapeStr, returnType);
                LOCAL_CACHE.put(redisKey, config);
                log.info("update local Cache:[{}]{}==>{}", channelTopic, redisKey, value);
            } else {
                log.warn("key is not exist:[{}]{}==>{}", channelTopic, redisKey, value);
            }
        });
        log.info("DcredisProxyFactory init done");
    }
}
