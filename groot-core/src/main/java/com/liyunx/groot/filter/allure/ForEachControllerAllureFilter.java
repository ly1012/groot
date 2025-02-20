package com.liyunx.groot.filter.allure;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.ExecuteSubStepsFilterChain;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.testelement.controller.ForEachController;
import io.qameta.allure.model.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.qameta.allure.Allure.getLifecycle;

public class ForEachControllerAllureFilter implements AllureFilter {

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof ForEachController;
    }

    @Override
    public void doRun(ContextWrapper ctx, RunFilterChain chain) {
        ForEachController controller = (ForEachController) ctx.getTestElement();
        AllureFilter.step(() -> controller.getRunning().getName(), uuid -> {
            chain.doRun(ctx);
        });
    }

    @Override
    public void doExecuteSubSteps(ContextWrapper ctx, ExecuteSubStepsFilterChain chain) {
        ForEachController controller = (ForEachController) ctx.getTestElement();
        int index = controller.getCurrentIterationIndex();
        int loopCount = controller.getLoopCount();
        Map<String, Object> data = controller.getColumnData();

        AllureFilter.step(() -> "第 " + loopCount + " 次循环 : 第" + index + "行数据", uuid -> {
            addParameters(uuid, data);
            chain.doExecuteSubSteps(ctx);
        });
    }

    private static void addParameters(String uuid, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        getLifecycle().updateStep(uuid, stepResult -> {
            List<Parameter> parameters = new ArrayList<>();
            params.forEach((name, value) -> {
                parameters.add(new Parameter()
                    .setName(name)
                    .setValue(objToString(value))
                    .setExcluded(null)
                    .setMode(null));
            });
            stepResult.setParameters(parameters);
        });
    }

    private static String objToString(Object value) {
        if (value instanceof String)
            return (String) value;
        return JSON.toJSONString(value);
    }

}
