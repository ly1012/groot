package com.liyunx.groot.testelement.sampler;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.builder.*;
import com.liyunx.groot.context.ContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 调试 Sampler，输出 debug 对象的字符串表示
 * TODO 开发中，需要重新设计
 */
@KeyWord(DebugSampler.KEY)
@Deprecated
public class DebugSampler extends AbstractSampler<DebugSampler, DefaultSampleResult> {

    private static final Logger log = LoggerFactory.getLogger(DebugSampler.class);

    public static final String KEY = "debug";

    @JSONField(name = "debug")
    private Object debug;

    public DebugSampler() {
    }

    private DebugSampler(Builder builder) {
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
        if (debug == null || (debug instanceof String && ((String) debug).trim().isEmpty()))
            return;
        log.info(contextWrapper.evalAsString(String.valueOf(debug)));
    }

    @Override
    public DebugSampler copy() {
        DebugSampler self = super.copy();
        self.debug = debug;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (DebugSampler)
    // ---------------------------------------------------------------------

    public Object getDebug() {
        return debug;
    }

    public void setDebug(Object debug) {
        this.debug = debug;
    }

    // ---------------------------------------------------------------------
    // Builder (DebugSampler.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractSampler.Builder<
        DebugSampler, Builder,
        CommonConfigBuilder,
        CommonPreProcessorsBuilder,
        CommonPostProcessorsBuilder, CommonExtractorsBuilder, CommonAssertionsBuilder> {

        @Override
        protected CommonConfigBuilder getConfigBuilder() {
            return new CommonConfigBuilder();
        }

        @Override
        protected CommonPreProcessorsBuilder getSetupBuilder(ContextWrapper ctx) {
            return new CommonPreProcessorsBuilder(ctx);
        }

        @Override
        protected CommonExtractorsBuilder getExtractBuilder(ContextWrapper ctx) {
            return new CommonExtractorsBuilder(ctx);
        }

        @Override
        protected CommonAssertionsBuilder getAssertBuilder(ContextWrapper ctx) {
            return new CommonAssertionsBuilder(ctx);
        }

        @Override
        protected CommonPostProcessorsBuilder getTeardownBuilder(ContextWrapper ctx) {
            return new CommonPostProcessorsBuilder(this, ctx);
        }

        @Override
        public DebugSampler build() {
            return new DebugSampler(this);
        }
    }

}
