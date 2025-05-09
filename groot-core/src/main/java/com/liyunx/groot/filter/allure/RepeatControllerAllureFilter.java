package com.liyunx.groot.filter.allure;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.ExecuteSubStepsFilterChain;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.testelement.controller.RepeatController;

public class RepeatControllerAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof RepeatController;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        RepeatController controller = (RepeatController) ctx.getTestElement();
        AllureFilter.step(() -> controller.getRunning().getName(), uuid -> {
            chain.doRun(ctx);
        });
    }

    @Override
    public void doExecuteSubSteps(ContextWrapper ctx, ExecuteSubStepsFilterChain chain) {
        RepeatController controller = (RepeatController) ctx.getTestElement();
        int loopCount = controller.getLoopCount();

        AllureFilter.step(() -> "第 " + loopCount + " 次循环", uuid -> {
            chain.doExecuteSubSteps(ctx);
        });
    }

}
