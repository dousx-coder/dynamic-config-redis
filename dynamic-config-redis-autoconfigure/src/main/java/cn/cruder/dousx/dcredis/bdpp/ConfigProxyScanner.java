package cn.cruder.dousx.dcredis.bdpp;

import cn.cruder.dousx.dcredis.adapter.ConfigProxyFactoryAdapter;
import cn.cruder.dousx.dcredis.annotation.DynamicConfig;
import cn.cruder.dousx.dcredis.factory.ConfigProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Set;


public class ConfigProxyScanner implements BeanDefinitionRegistryPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(ConfigProxyScanner.class);

    private String[] basePackages;

    // 新增：注入 ResourceLoader
    private ResourceLoader resourceLoader;

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) {
        // 创建自定义扫描器（支持接口）
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) { // 禁用默认过滤器
                    @Override
                    protected boolean isCandidateComponent(@Nonnull AnnotatedBeanDefinition beanDefinition) {
                        return true; // 允许接口类型
                    }
                };
        // 关键：设置资源加载器
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DynamicConfig.class));
        // 显式指定资源模式
        scanner.setResourcePattern("**/*.class");

        // 扫描逻辑（保持不变）
        Arrays.stream(basePackages).forEach(basePackage -> {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                try {
                    Class<?> clazz = Class.forName(candidate.getBeanClassName());
                    if (clazz.isInterface()) {
                        // 关键修改：使用 ConfigProxyFactory
                        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConfigProxyFactoryAdapter.class);

                        // 指定构造方法 参数
                        builder.addConstructorArgValue(clazz);
                        // 指定依赖ConfigProxyFactory
                        builder.addDependsOn(ConfigProxyFactory.BEAN_NAME);

                        // Bean 名称策略
                        String beanName = StringUtils.uncapitalize(clazz.getSimpleName()) + "$ConfigProxy";
                        log.info("Class:{} beanName:{}", candidate.getBeanClassName(), beanName);

                        // Bean 名称生成策略
                        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }
}