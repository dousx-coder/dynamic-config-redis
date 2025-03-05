package cn.cruder.dousx.dcredis.pojo;

import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;


public class UpdateRedisConfigParam implements Serializable {
    /**
     * {@link DcredisProperty#key()}
     */
    private final String key;

    private final Object config;

    public UpdateRedisConfigParam(String key, Object config) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("annotationKey is blank");
        }
        this.key = key;
        this.config = config;
    }

    public String getKey() {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("annotationKey is blank");
        }
        return key;
    }

    public Object getConfig() {
        return config;
    }
}
