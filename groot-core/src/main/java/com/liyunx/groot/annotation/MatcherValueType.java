package com.liyunx.groot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MatcherValueType {

    /**
     * 预期值的默认类型，仅支持八大基本类型和 String 类型，其他情况不支持该注解
     */
    Class<?> value();

}
