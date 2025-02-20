package com.liyunx.groot.processor.assertion;

import com.liyunx.groot.builder.TestBuilder;
import com.liyunx.groot.processor.AbstractProcessor;

/**
 * 通用字段和逻辑处理
 *
 * <p>实现类尽量继承该类，方便以后可能的扩展
 */
public abstract class AbstractAssertion extends AbstractProcessor implements Assertion {

    public AbstractAssertion() {}

    protected AbstractAssertion(Builder<?, ?> builder) {
        super(builder);
    }

    //@formatter:off
    public static abstract class Builder<U extends AbstractAssertion,
                                         SELF extends Builder<U, SELF>>
        extends AbstractProcessor.Builder<U, SELF>
        implements TestBuilder<U>
    //@formatter:on
    {

    }

}
