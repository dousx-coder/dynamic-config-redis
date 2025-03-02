package cn.cruder.dousx.dcredis.factory;


import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.component.DcredisKeyComponent;
import cn.cruder.dousx.dcredis.constant.TopicConstant;
import cn.cruder.tools.json.JsonUtilPool;
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
                new ConfigInvocationHandler(configInterface)
        );
        return configInterface.cast(proxy);
    }

    private class ConfigInvocationHandler implements InvocationHandler {

        public ConfigInvocationHandler(Class<?> configInterface) {
            subscribeToRedis();
        }

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
                        String sv = "";
                        Object redisValue = redissonClient.getBucket(redisKey).get();
                        if (redisValue != null) {
                            sv = (String) redisValue;
                        }
                        if (StringUtils.isBlank(sv)) {
                            sv = dcredisProperty.defaultValue();
                            log.debug("取默认配置：{} ==> {}", redisKey, sv);
                        }
                        Class<?> returnType = method.getReturnType();
                        config = JsonUtilPool.parseObject(sv, returnType);
                        LOCAL_CACHE.put(redisKey, config);
                    }
                }
            }
            return config;
        }

        private void subscribeToRedis() {
            RTopic topic = redissonClient.getTopic(TopicConstant.CONFIG_TOPIC);
            topic.addListener(String.class, (channel, msg) -> {
                // msg is key
                String value = (String) redissonClient.getBucket(msg).get();
                if (LOCAL_CACHE.containsKey(msg)) {
                    LOCAL_CACHE.put(msg, value);
                    log.info("update local Cache:{}==>{}", msg, value);
                } else {
                    log.warn("key is not exist:{}==>{}", msg, value);
                }
            });
        }
    }
}
