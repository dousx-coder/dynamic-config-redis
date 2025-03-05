package cn.cruder.dousx.dcredis.component;


import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.properties.DcredisProperties;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 辅助
 */
@Component
public class DcredisAuxiliary {
    /**
     * 本地缓存
     */

    @Autowired
    private DcredisProperties dcredisProperties;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取redis key
     *
     * @param annotationKey {@link DcredisProperty#key()}的值
     * @return redis key
     */
    public String getRedisConfigKey(String annotationKey) {
        String nameSpace = dcredisProperties.getNameSpace();
        if (StringUtils.isBlank(nameSpace)) {
            throw new IllegalArgumentException("nameSpace is blank");
        }
        return nameSpace + ":" + annotationKey;
    }

    public RLock getDcredisLock() {
        String key = "dcredis:lock:" + dcredisProperties.getNameSpace();
        return redissonClient.getLock(key);
    }
}
