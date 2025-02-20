package com.liyunx.groot.processor;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;

/**
 * 后置函数处理器
 */
@KeyWord(HooksPostProcessor.KEY)
public class HooksPostProcessor extends AbstractHooksProcessor implements PostProcessor {

    public HooksPostProcessor() {
    }

    private HooksPostProcessor(Builder builder) {
        super(builder);
    }

    @Override
    public ValidateResult validate() {
        return super.validate();
    }

    @Override
    public void process(ContextWrapper ctx) {
        super.process(ctx, "后置");
    }

    public static class Builder extends AbstractHooksProcessor.Builder<HooksPostProcessor, Builder> {

        @Override
        public HooksPostProcessor build() {
            return new HooksPostProcessor(this);
        }

    }

}
