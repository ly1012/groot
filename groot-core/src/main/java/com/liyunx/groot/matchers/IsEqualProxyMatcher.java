package com.liyunx.groot.matchers;

import com.liyunx.groot.context.ContextWrapper;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;

/**
 * {@link IsEqual} 代理
 *
 * @param <T> 值类型
 */
@SuppressWarnings("unchecked")
public class IsEqualProxyMatcher<T> extends ProxyMatcher<T> {

    private String expected;

    public IsEqualProxyMatcher() {
    }

    public IsEqualProxyMatcher(String expected) {
        this.expected = expected;
    }

    public IsEqualProxyMatcher(Class<T> typeClass, String expected) {
        this.matcherValueType = typeClass;
        this.expected = expected;
    }

    @Override
    public Matcher<T> toMatcher(ContextWrapper ctx) {
        Object _expectedValue = ctx.eval(expected);
        T expectedValue = valueAsType(matcherValueType, _expectedValue);
        return Matchers.equalTo(expectedValue);
    }


    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

}
