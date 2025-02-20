package com.liyunx.groot.filter.allure;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.testelement.controller.GroupController;

public class GroupControllerAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof GroupController;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        GroupController controller = (GroupController) ctx.getTestElement();
        AllureFilter.step(() -> controller.getRunning().getName(), uuid -> {
            chain.doRun(ctx);
        });
    }

}
