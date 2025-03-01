package cn.cruder.dousx.dcredis.component;


import cn.cruder.dousx.dcredis.annotation.DynamicConfigProperty;
import cn.cruder.dousx.dcredis.properties.ConfigProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyComponent {
    @Autowired
    private ConfigProperties configProperties;

    /**
     * 获取redis key
     *
     * @param annotationKey {@link DynamicConfigProperty#key()}的值
     * @return redis key
     */
    public String getRedisKey(String annotationKey) {
        String nameSpace = configProperties.getNameSpace();
        if (StringUtils.isBlank(nameSpace)) {
            throw new IllegalArgumentException("nameSpace is blank");
        }
        return nameSpace + ":" + annotationKey;
    }

}
