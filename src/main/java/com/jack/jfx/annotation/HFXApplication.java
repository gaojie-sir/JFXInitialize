package com.jack.jfx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author gj
 * @date 2021/4/26 16:05
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HFXApplication {
    /**
     * css路径
     *
     * @return
     */
    String css();

    /**
     * 开启css自动装配
     *
     * @return
     */
    boolean enableCssAuto() default true;
}
