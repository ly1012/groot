package com.liyunx.groot.matchers;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.liyunx.groot.matchers.ProxyMatcher.toMatcherIfProxy;

/**
 * {@link Matchers} 类的代理实现，方法名和 Matchers 中的方法名相同，但参数支持表达式。
 */
public class ProxyMatchers {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SafeVarargs
    public static <T> Matcher<T> allOf(Matcher<? super T>... matchers) {
        return allOf((Iterable) Arrays.asList(matchers));
    }

    public static <T> Matcher<T> allOf(Iterable<Matcher<? super T>> matchers) {
        return ProxyMatcherFactory.create((self, ctx) -> {
            List<Matcher<? super T>> matchers2 = new ArrayList<>();
            for (Matcher<? super T> matcher : matchers) {
                matchers2.add(toMatcherIfProxy(ctx, self.matcherValueTypes, matcher));
            }
            return Matchers.allOf(matchers2);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SafeVarargs
    public static <T> Matcher<T> anyOf(Matcher<? super T>... matchers) {
        return anyOf((Iterable) Arrays.asList(matchers));
    }

    public static <T> Matcher<T> anyOf(Iterable<Matcher<? super T>> matchers) {
        return ProxyMatcherFactory.create((self, ctx) -> {
            List<Matcher<? super T>> matchers2 = new ArrayList<>();
            for (Matcher<? super T> matcher : matchers) {
                matchers2.add(toMatcherIfProxy(ctx, self.matcherValueTypes, matcher));
            }
            return Matchers.anyOf(matchers2);
        });
    }

    public static <T> Matcher<T> equalTo(String expected) {
        return equalTo(null, expected);
    }

    public static <T> Matcher<T> equalTo(Class<T> typeClass, String expected) {
        return ProxyMatcherFactory.create(typeClass, expected, Matchers::equalTo);
    }

    public static Matcher<String> containsString(String expected) {
        return ProxyMatcherFactory.create(String.class, expected, Matchers::containsString);
    }

    public static <T extends Comparable<T>> Matcher<T> greaterThan(String expected) {
        return greaterThan(null, expected);
    }

    public static <T extends Comparable<T>> Matcher<T> greaterThan(Class<T> typeClass, String expected) {
        return ProxyMatcherFactory.create(typeClass, expected, Matchers::greaterThan);
    }

}


