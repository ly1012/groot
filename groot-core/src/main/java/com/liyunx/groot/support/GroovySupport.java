package com.liyunx.groot.support;

import groovy.lang.Closure;

/**
 * Groovy 集成辅助类
 */
public class GroovySupport {

    /**
     * 指定闭包的委托对象并调用闭包，使用 {@link Closure#DELEGATE_ONLY} 策略。
     *
     * @param cl       闭包，无参数
     * @param delegate 委托对象
     */
    public static void call(Closure<?> cl, Object delegate) {
        Closure<?> code = (Closure<?>) cl.clone();
        code.setDelegate(delegate);
        code.setResolveStrategy(Closure.DELEGATE_ONLY);
        code.call();
    }

}
