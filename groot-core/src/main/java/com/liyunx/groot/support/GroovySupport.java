package com.liyunx.groot.support;

import com.liyunx.groot.builder.TestBuilder;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.lang.reflect.InvocationTargetException;

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

    /**
     * 闭包定义辅助方法，通过方法调用创建闭包，指定委托对象类型后闭包中调用委托对象的方法时，IDEA 会给出提示。
     *
     * @param type    委托对象的类型
     * @param closure 闭包
     * @param <T>     委托对象的类型
     * @return 闭包
     */
    public static <T> Closure<T> defClosure(Class<T> type,
                                            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "T") Closure<T> closure) {
        return closure;
    }

    public static <T extends TestBuilder<?>> T defBuilder(
        Class<T> type,
        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "T") Closure<?> closure
    ) {
        T builder = null;
        try {
            builder = type.getConstructor().newInstance();
            call(closure, builder);
            return builder;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 合并多个闭包为一个闭包数组。
     *
     * @param beforeClosure 最先执行的闭包
     * @param closures      中间执行的闭包数组
     * @param afterClosure  最后执行的闭包
     * @return 合并后的闭包数组
     */
    @SuppressWarnings("rawtypes")
    public static Closure[] mergeClosures(Closure beforeClosure, Closure[] closures, Closure afterClosure) {
        int extraLength = (beforeClosure != null ? 1 : 0) + (afterClosure != null ? 1 : 0);
        Closure[] mergedClosures = new Closure[closures.length + extraLength];

        int offset = 0;
        if (beforeClosure != null) {
            mergedClosures[0] = beforeClosure;
            offset++;
        }
        System.arraycopy(closures, 0, mergedClosures, offset, closures.length);
        if (afterClosure != null) {
            mergedClosures[mergedClosures.length - 1] = afterClosure;
        }
        return mergedClosures;
    }

}
