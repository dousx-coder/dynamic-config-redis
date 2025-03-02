package cn.cruder.dousx.dcredis.annotation;

import cn.cruder.dousx.dcredis.registry.DcredisProxyRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启配置，并指定扫描的包名
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(DcredisProxyRegistrar.class)
public @interface EnableDcredis {
    /**
     * 需要扫描的包
     * <br/>
     * 默认是当前包及子包
     *
     * @return 需要扫描的包
     */
    String[] basePackages() default {};
}