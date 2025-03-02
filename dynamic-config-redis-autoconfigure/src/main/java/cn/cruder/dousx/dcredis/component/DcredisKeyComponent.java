package cn.cruder.dousx.dcredis.component;


import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.properties.DcredisProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DcredisKeyComponent {

    @Autowired
    private DcredisProperties dcredisProperties;

    /**
     * 获取redis key
     *
     * @param annotationKey {@link DcredisProperty#key()}的值
     * @return redis key
     */
    public String getRedisKey(String annotationKey) {
        String nameSpace = dcredisProperties.getNameSpace();
        if (StringUtils.isBlank(nameSpace)) {
            throw new IllegalArgumentException("nameSpace is blank");
        }
        return nameSpace + ":" + annotationKey;
    }

}
