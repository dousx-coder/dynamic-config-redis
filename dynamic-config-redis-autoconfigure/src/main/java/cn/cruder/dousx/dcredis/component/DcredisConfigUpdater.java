package cn.cruder.dousx.dcredis.component;


import cn.cruder.dousx.dcredis.constant.TopicConstant;
import cn.cruder.dousx.dcredis.pojo.UpdateRedisConfigParam;
import cn.hutool.core.util.ObjectUtil;
import org.redisson.api.RLock;
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
    private DcredisAuxiliary dcredisAuxiliary;

    /**
     * 更新redis中的配置，并发布消息
     *
     * @param param {@link UpdateRedisConfigParam}
     */
    public void updateRedisConfig(UpdateRedisConfigParam param) {
        String annotationKey = param.getKey();
        Object newConfig = param.getConfig();
        if (ObjectUtil.hasEmpty(newConfig, annotationKey)) {
            throw new IllegalArgumentException(String.format("[%s] config is null", annotationKey));
        }
        RLock lock = dcredisAuxiliary.getDcredisLock();
        lock.lock();
        try {
            String redisKey = dcredisAuxiliary.getRedisConfigKey(annotationKey);
            redissonClient.getBucket(redisKey).set(newConfig);
            // 发布消息
            redissonClient.getTopic(TopicConstant.CONFIG_TOPIC).publish(redisKey);
            log.info("{}==>{}", redisKey, newConfig);
        } finally {
            lock.unlock();
        }
    }
}