package com.liyunx.groot.processor;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;

/**
 * 前置函数处理器
 */
@KeyWord(HooksPreProcessor.KEY)
public class HooksPreProcessor extends AbstractHooksProcessor implements PreProcessor {

    public HooksPreProcessor() {
    }

    private HooksPreProcessor(Builder builder) {
        super(builder);
    }

    @Override
    public ValidateResult validate() {
        return super.validate();
    }

    @Override
    public void process(ContextWrapper ctx) {
        super.process(ctx, "前置");
    }

    public static class Builder extends AbstractHooksProcessor.Builder<HooksPreProcessor, Builder> {

        @Override
        public HooksPreProcessor build() {
            return new HooksPreProcessor(this);
        }

    }

}
