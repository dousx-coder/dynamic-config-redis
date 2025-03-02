package cn.cruder.dousx.dcredis.annotation;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DcredisProperty {
    /**
     * The property name.
     */
    String key();

    /**
     * 默认值
     *
     * @return 默认值
     */
    String defaultValue() default "";

}
