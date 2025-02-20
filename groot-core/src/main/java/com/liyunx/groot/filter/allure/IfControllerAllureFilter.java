package com.liyunx.groot.filter.allure;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.testelement.controller.IfController;
import io.qameta.allure.model.Status;

public class IfControllerAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof IfController;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        IfController controller = (IfController) ctx.getTestElement();
        AllureFilter.step(
            () -> controller.getRunning().getName(),
            () -> controller.getSatisfied() ? Status.PASSED : Status.SKIPPED,
            uuid -> chain.doRun(ctx)
        );
    }

}
