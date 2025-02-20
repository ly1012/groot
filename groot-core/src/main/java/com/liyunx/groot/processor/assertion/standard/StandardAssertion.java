package com.liyunx.groot.processor.assertion.standard;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.processor.assertion.AbstractAssertion;

/**
 * 标准断言
 *
 * <p>标准断言元件支持位置参数写法（配置风格用例），依次为：实际值（要检验的值）、预期值、断言参数
 *
 * <p>实际值类型和预期值类型未必相同，比如 hasSize("abc", 3)，实际值为 String 类型，预期值为 Integer 类型
 */
public abstract class StandardAssertion<ACTUAL, EXPECTED> extends AbstractAssertion {

    @JSONField(name = "check")
    protected ACTUAL actualValue;

    @JSONField(name = "expect")
    protected EXPECTED expectedValue;

    public StandardAssertion() {
    }

    protected StandardAssertion(Builder<?, ACTUAL, EXPECTED, ?> builder) {
        super(builder);
        this.actualValue = builder.actualValue;
        this.expectedValue = builder.expectedValue;
    }

    public StandardAssertion(ACTUAL actualValue, EXPECTED expectedValue) {
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
    }

    public ACTUAL getActualValue() {
        return actualValue;
    }

    public void setActualValue(ACTUAL actualValue) {
        this.actualValue = actualValue;
    }

    public EXPECTED getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(EXPECTED expectedValue) {
        this.expectedValue = expectedValue;
    }

    //@formatter:off
    public static abstract class Builder<U extends StandardAssertion<ACTUAL, EXPECTED>,
                                         ACTUAL,
                                         EXPECTED,
                                         SELF extends Builder<U, ACTUAL, EXPECTED, SELF>>
        extends AbstractAssertion.Builder<U, SELF>
    //@formatter:on
    {

        protected ACTUAL actualValue;
        protected EXPECTED expectedValue;

        public SELF check(ACTUAL actualValue) {
            this.actualValue = actualValue;
            return self;
        }

        public SELF expect(EXPECTED expectedValue) {
            this.expectedValue = expectedValue;
            return self;
        }

    }

}
