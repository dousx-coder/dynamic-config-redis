package cn.cruder.dousx.dcredis.registry;

import cn.cruder.dousx.dcredis.annotation.EnableDynamicConfig;
import cn.cruder.dousx.dcredis.bdpp.ConfigProxyScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;
import java.util.Map;


public class ConfigProxyRegistrar implements ImportBeanDefinitionRegistrar {
    private static final Logger log = LoggerFactory.getLogger(ConfigProxyRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableDynamicConfig.class.getName());
        if (attributes == null) {
            log.warn("Not EnableConfig");
            return;
        }
        String[] basePackages = (String[]) attributes.get("basePackages");
        if (basePackages == null || basePackages.length == 0) {
            basePackages = new String[]{ClassUtils.getPackageName(importingClassMetadata.getClassName())};
        }
        ConfigProxyScanner scanner = new ConfigProxyScanner();
        scanner.setBasePackages(basePackages);
        scanner.postProcessBeanDefinitionRegistry(registry);
    }
}