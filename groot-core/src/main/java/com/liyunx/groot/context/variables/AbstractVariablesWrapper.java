package com.liyunx.groot.context.variables;

import com.liyunx.groot.config.ConfigGroup;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.Context;
import com.liyunx.groot.context.TestRunContext;
import com.liyunx.groot.config.TestElementConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 变量包装类基础实现
 */
public abstract class AbstractVariablesWrapper implements VariablesWrapper {

    // 上下文链
    protected List<Context> contextChain;

    // 最后一个上下文
    protected Context lastContext;

    public AbstractVariablesWrapper(List<Context> contextChain) {
        this.contextChain = contextChain;
        this.lastContext = contextChain.get(contextChain.size() - 1);
    }

    public Object get(String name) {
        if (hasLastVariableConfigItem()){
            return getLastVariableConfigItem().get(name);
        }
        return null;
    }

    public Object put(String name, Object value) {
        return getLastVariableConfigItem().put(name, value);
    }

    public Object remove(String name) {
        if (hasLastVariableConfigItem()){
            return getLastVariableConfigItem().remove(name);
        }
        return null;
    }

    @Override
    public Map<String, Object> mergeVariables() {
        // TODO 这里也许可以改成 HashMap，暂时没发现需要线程共享的场景
        Map<String, Object> variables = new ConcurrentHashMap<>();
        // 合并上下文链中的变量，后加入上下文的变量会覆盖前面上下文的变量
        for (int i = 0; i < contextChain.size(); i++) {
            ConfigGroup configGroup = contextChain.get(i).getConfigGroup();
            if (configGroup != null) {
                Map<String, Object> ctxVars = configGroup.getVariableConfigItem();
                if (ctxVars != null) {
                    variables.putAll(ctxVars);
                }
            }
        }
        return variables;
    }

    private boolean hasLastVariableConfigItem() {
        if (lastContext.getConfigGroup() == null) {
            return false;
        }

        return lastContext.getConfigGroup().getVariableConfigItem() != null;
    }

    public VariableConfigItem getLastVariableConfigItem() {
        ConfigGroup lastConfigGroup = lastContext.getConfigGroup();

        if (lastConfigGroup == null) {
            lastConfigGroup = new TestElementConfig();
            ((TestRunContext) lastContext).setConfigGroup(lastConfigGroup);
        }

        VariableConfigItem lastVariableConfigItem = lastConfigGroup.getVariableConfigItem();
        if (lastVariableConfigItem != null) {
            return lastVariableConfigItem;
        }

        lastVariableConfigItem = new VariableConfigItem();
        lastConfigGroup.put(VariableConfigItem.KEY, lastVariableConfigItem);
        return lastVariableConfigItem;
    }

    @Override
    public String toString() {
        return mergeVariables().toString();
    }

}
