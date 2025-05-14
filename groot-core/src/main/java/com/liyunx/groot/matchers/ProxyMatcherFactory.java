package com.liyunx.groot.matchers;

import com.liyunx.groot.context.ContextWrapper;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.liyunx.groot.matchers.ProxyMatcher.valueAsType;
import static java.util.Objects.nonNull;

@SuppressWarnings("rawtypes")
public class ProxyMatcherFactory {

    /**
     * ProxyMatcher 模板类，不同的参数代表不同的 ProxyMatcher 实现
     */
    public static class ConcreteProxyMatcher<T> extends ProxyMatcher<T> {

        private final BiFunction<ProxyMatcher<T>, ContextWrapper, Matcher<T>> consumer;

        public ConcreteProxyMatcher(List<Class> valueTypes,
                                    BiFunction<ProxyMatcher<T>, ContextWrapper, Matcher<T>> consumer) {
            this.matcherValueTypes = valueTypes;
            this.consumer = consumer;
        }

        @Override
        public Matcher<T> toMatcher(ContextWrapper ctx) {
            return consumer.apply(this, ctx);
        }
    }

    public static <T> ConcreteProxyMatcher<T> create(List<Class> valueTypes,
                                                     BiFunction<ProxyMatcher<T>, ContextWrapper, Matcher<T>> consumer) {
        return new ConcreteProxyMatcher<>(valueTypes, consumer);
    }

    public static <T> ConcreteProxyMatcher<T> create(BiFunction<ProxyMatcher<T>, ContextWrapper, Matcher<T>> consumer) {
        return create((List<Class>) null, consumer);
    }

    public static <T> ConcreteProxyMatcher<T> create(Class<T> valueType,
                                                     BiFunction<ProxyMatcher<T>, ContextWrapper, Matcher<T>> consumer) {
        List<Class> valueTypes = null;
        if (nonNull(valueType)) {
            valueTypes = new ArrayList<>();
            valueTypes.add(valueType);
        }
        return create(valueTypes, consumer);
    }

    /**
     * 创建一个具体的 ProxyMatcher 对象
     *
     * @param valueType 参数值类型，expected 计算后的值转为该类型传递给 consumer
     * @param expected  参数值（可能包含表达式）
     * @param consumer  具体的逻辑
     * @param <T>       参数值类型
     * @return ProxyMatcher 对象
     */
    public static <T> ConcreteProxyMatcher<T> create(Class<T> valueType,
                                                     String expected,
                                                     Function<T, Matcher<T>> consumer) {
        return create(valueType, (self, ctx) -> {
            Object _expectedValue = ctx.eval(expected);
            T expectedValue = valueAsType(self.getFirstClass(), _expectedValue);
            return consumer.apply(expectedValue);
        });
    }

}
