package cn.cruder.dousx.dcredis.component;


import cn.cruder.dousx.dcredis.constant.TopicConstant;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigUpdater {
    private static final Logger log = LoggerFactory.getLogger(ConfigUpdater.class);

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisKeyComponent redisKeyComponent;

    public void updateRedisConfig(String annotationKey, String value) {
        String redisKey = redisKeyComponent.getRedisKey(annotationKey);
        //更新缓存
        redissonClient.getBucket(redisKey).set(value);
        // 发布消息
        redissonClient.getTopic(TopicConstant.CONFIG_TOPIC).publish(redisKey);
        log.info("{}==>{}", redisKey, value);
    }
}