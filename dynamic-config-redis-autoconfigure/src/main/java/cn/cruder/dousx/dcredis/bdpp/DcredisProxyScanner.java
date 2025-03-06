package cn.cruder.dousx.dcredis.bdpp;

import cn.cruder.dousx.dcredis.adapter.DcredisProxyFactoryAdapter;
import cn.cruder.dousx.dcredis.annotation.DcredisConfig;
import cn.cruder.dousx.dcredis.annotation.DcredisProperty;
import cn.cruder.dousx.dcredis.factory.DcredisProxyFactory;
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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


public class DcredisProxyScanner implements BeanDefinitionRegistryPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(DcredisProxyScanner.class);


    private Set<String> scanPackages;

    private ResourceLoader resourceLoader;

    public void setScanPackages(Set<String> scanPackages) {
        this.scanPackages = scanPackages;
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

        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DcredisConfig.class));
        // 显式指定资源模式
        scanner.setResourcePattern("**/*.class");
        Set<String> allKey = new HashSet<>();
        // 扫描逻辑
        scanPackages.forEach(basePackage -> {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                try {
                    String beanClassName = candidate.getBeanClassName();
                    Class<?> clazz = Class.forName(beanClassName);
                    if (!clazz.isInterface()) {
                        continue;
                    }
                    if (clazz.getAnnotation(DcredisConfig.class) == null) {
                        continue;
                    }

                    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DcredisProxyFactoryAdapter.class);
                    Method[] methods = clazz.getMethods();
                    if (methods.length == 0) {
                        String error = String.format("[%s]Config definitions are missing", beanClassName);
                        throw new UnsupportedOperationException(error);
                    }
                    for (Method method : methods) {
                        DcredisProperty dcredisProperty = method.getAnnotation(DcredisProperty.class);
                        if (dcredisProperty == null) {
                            String errors = String.format("[%s]DcredisProperty is missing", method.getName());
                            throw new UnsupportedOperationException(errors);
                        }
                        String dcKey = dcredisProperty.key();
                        if (!allKey.add(dcKey)) {
                            String error = String.format("[%s]DcredisProperty duplicate key：%s", method.getName(), dcKey);
                            throw new UnsupportedOperationException(error);
                        }
                    }
                    // 指定DcredisProxyFactoryAdapter构造方法参数
                    builder.addConstructorArgValue(clazz);
                    // 指定依赖DcredisProxyFactory
                    builder.addDependsOn(DcredisProxyFactory.BEAN_NAME);

                    // Bean 名称策略
                    String beanName = StringUtils.uncapitalize(clazz.getSimpleName()) + "$DcredisProxy";

                    // Bean 名称生成策略
                    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
                    log.info("registerBeanDefinition Class:{} beanName:{}", beanClassName, beanName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }
}