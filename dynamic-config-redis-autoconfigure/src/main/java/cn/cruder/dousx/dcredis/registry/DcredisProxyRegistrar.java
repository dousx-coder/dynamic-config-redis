package cn.cruder.dousx.dcredis.registry;

import cn.cruder.dousx.dcredis.annotation.EnableDcredis;
import cn.cruder.dousx.dcredis.bdpp.DcredisProxyScanner;
import cn.cruder.tools.json.JsonUtilPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;
import java.util.*;


public class DcredisProxyRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String PACKAGE_SPLIT = ".";
    private static final Logger log = LoggerFactory.getLogger(DcredisProxyRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableDcredis.class.getName());
        if (attributes == null) {
            log.warn("Not EnableConfig");
            return;
        }
        String[] basePackages = (String[]) attributes.get("basePackages");
        if (basePackages == null || basePackages.length == 0) {
            basePackages = new String[]{ClassUtils.getPackageName(importingClassMetadata.getClassName())};
        }
        List<String> list = Arrays.stream(basePackages).sorted(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.split(PACKAGE_SPLIT).length - o2.split(PACKAGE_SPLIT).length;
            }
        }).toList();
        Set<String> scanPackages = new HashSet<>();
        // O²复杂度不影响性能(这里一般不会配置太多)
        for (String child : list) {
            for (String parent : list) {
                if (child.startsWith(parent)) {
                    // s是b的子包，添加b即可
                    scanPackages.add(parent);
                    break;
                }
            }
        }
        log.info("dcredis basePackages,{} ===> {}", JsonUtilPool.toJsonString(basePackages), JsonUtilPool.toJsonString(scanPackages));
        DcredisProxyScanner scanner = new DcredisProxyScanner();
        scanner.setScanPackages(scanPackages);
        scanner.postProcessBeanDefinitionRegistry(registry);
    }
}