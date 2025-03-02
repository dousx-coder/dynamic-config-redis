package cn.cruder.dousx.dcredis.component;


import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.constant.TopicConstant;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DcredisConfigUpdater {
    private static final Logger log = LoggerFactory.getLogger(DcredisConfigUpdater.class);

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private DcredisKeyComponent dcredisKeyComponent;

    /**
     * 更新redis中的配置，并发布消息
     *
     * @param annotationKey {@link DcredisProperty#key()}的值
     * @param value         配置值
     */
    public void updateRedisConfig(String annotationKey, String value) {
        String redisKey = dcredisKeyComponent.getRedisKey(annotationKey);
        //更新缓存
        redissonClient.getBucket(redisKey).set(value);
        // 发布消息
        redissonClient.getTopic(TopicConstant.CONFIG_TOPIC).publish(redisKey);
        log.info("{}==>{}", redisKey, value);
    }
}