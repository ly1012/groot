package com.liyunx.groot.filter.allure;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.testelement.sampler.DebugSampler;

public class DebugSamplerAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof DebugSampler;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        DebugSampler sampler = (DebugSampler) ctx.getTestElement();
        AllureFilter.step(() -> sampler.getRunning().getName(), uuid -> {
            chain.doRun(ctx);
        });
    }

}
