package com.liyunx.groot.protocol.http.filter;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.filter.allure.AllureFilter;
import com.liyunx.groot.protocol.http.HttpSampler;

public class HttpAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof HttpSampler;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        HttpSampler sampler = (HttpSampler) ctx.getTestElement();
        AllureFilter.step(() -> sampler.getRunning().getName(), uuid -> {
            chain.doRun(ctx);

            // TODO Allure verbose level or http log switch?
            //HttpSampleResult sampleResult = (HttpSampleResult) ctx.getTestResult();
            //Allure.addAttachment("Request", "text/html", "ccc");
            //Allure.addAttachment("Response", "text/html", "ccc");
        });
    }

}
