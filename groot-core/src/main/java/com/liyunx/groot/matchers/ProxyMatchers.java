package com.liyunx.groot.matchers;

import org.hamcrest.Matcher;

public class ProxyMatchers {

    public static <T> Matcher<T> equalTo(String expected) {
        return new IsEqualProxyMatcher<>(expected);
    }

    public static <T> Matcher<T> equalTo(Class<T> typeClass, String expected) {
        return new IsEqualProxyMatcher<>(typeClass, expected);
    }

}


