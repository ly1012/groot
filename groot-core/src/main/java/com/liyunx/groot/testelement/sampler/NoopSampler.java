package com.liyunx.groot.testelement.sampler;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.builder.*;
import com.liyunx.groot.context.ContextWrapper;

/**
 * Noop Sampler 不会执行任何操作（除了配置和前后置），即一个没有任何原子能力的 Sampler
 */
@KeyWord(NoopSampler.KEY)
public class NoopSampler extends AbstractSampler<NoopSampler, DefaultSampleResult> {

    public static final String KEY = "noop";

    @JSONField(name = KEY)
    private Object noop;

    public NoopSampler() {
    }

    private NoopSampler(Builder builder) {
        super(builder);
    }

    // ---------------------------------------------------------------------
    // 重写 AbstractTestElement 方法
    // ---------------------------------------------------------------------

    @Override
    protected DefaultSampleResult createTestResult() {
        return new DefaultSampleResult();
    }

    // ---------------------------------------------------------------------
    // 重写 AbstractSampler 方法
    // ---------------------------------------------------------------------

    @Override
    protected void sample(ContextWrapper contextWrapper, DefaultSampleResult result) {
        // noop No Operation Performed
    }

    @Override
    public NoopSampler copy() {
        NoopSampler self = super.copy();
        self.noop = noop;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (DebugSampler)
    // ---------------------------------------------------------------------

    public Object getNoop() {
        return noop;
    }

    public void setNoop(Object noop) {
        this.noop = noop;
    }

    // ---------------------------------------------------------------------
    // Builder (DebugSampler.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractSampler.Builder<
        NoopSampler, Builder,
        CommonConfigBuilder,
        CommonPreProcessorsBuilder<NoopSampler>,
        CommonPostProcessorsBuilder<DefaultSampleResult>, CommonExtractorsBuilder<DefaultSampleResult>, CommonAssertionsBuilder<DefaultSampleResult>> {

        @Override
        protected CommonConfigBuilder getConfigBuilder() {
            return new CommonConfigBuilder();
        }

        @Override
        protected CommonPreProcessorsBuilder<NoopSampler> getSetupBuilder(ContextWrapper ctx) {
            return new CommonPreProcessorsBuilder<>(ctx);
        }

        @Override
        protected CommonPostProcessorsBuilder<DefaultSampleResult> getTeardownBuilder(ContextWrapper ctx) {
            return new CommonPostProcessorsBuilder<>(this, ctx);
        }

        @Override
        protected CommonExtractorsBuilder<DefaultSampleResult> getExtractBuilder(ContextWrapper ctx) {
            return new CommonExtractorsBuilder<>(ctx);
        }

        @Override
        protected CommonAssertionsBuilder<DefaultSampleResult> getAssertBuilder(ContextWrapper ctx) {
            return new CommonAssertionsBuilder<>(ctx);
        }

        @Override
        public NoopSampler build() {
            return new NoopSampler(this);
        }
    }

}
