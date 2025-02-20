package com.liyunx.groot.testng.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GrootDataSource 是一个组合注解，功能上等于 {@link GrootSupport} + {@link DataSource} + {@link DataFilter}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@GrootSupport
public @interface GrootDataSource {

    /**
     * {@link DataSource#value()}
     */
    String value() default "";

    /**
     * {@link DataFilter#slice()}
     */
    String slice() default "";

    /**
     * {@link DataFilter#expr()}
     */
    String expr() default "";

    boolean parallel() default false;

}
