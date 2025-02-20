package com.liyunx.groot.filter.allure;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.testelement.controller.RefTestCaseController;

public class RefTestCaseControllerAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof RefTestCaseController;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        RefTestCaseController controller = (RefTestCaseController) ctx.getTestElement();
        AllureFilter.step(() -> controller.getRunning().getName(), uuid -> {
            chain.doRun(ctx);
        });
    }

}
