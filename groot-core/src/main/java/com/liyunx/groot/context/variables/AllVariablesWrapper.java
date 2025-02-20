package com.liyunx.groot.context.variables;


import com.liyunx.groot.config.ConfigGroup;
import com.liyunx.groot.context.Context;

import java.util.List;
import java.util.Map;

/**
 * 变量包装类：包含上下文链中所有变量。
 *
 * <p>get/put/remove 操作应逐级查找，而不是合并上下文后查找，因为操作结果需要反映到上下文中。
 */
public class AllVariablesWrapper extends AbstractVariablesWrapper {

    public AllVariablesWrapper(List<Context> contextChain) {
        super(contextChain);
    }

    public Object get(String name) {
        //倒序查找
        for (int i = contextChain.size() - 1; i >= 0; i--) {
            Context ctx = contextChain.get(i);
            ConfigGroup configGroup = ctx.getConfigGroup();
            if (configGroup == null) continue;
            Map<String, Object> variables = configGroup.getVariableConfigItem();
            if (variables == null) continue;
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
        }
        // 上下文链中不存在该变量，直接返回 null
        return null;
    }

    // 如果有对应的变量：assign 赋值（变量层级为当初声明的层级）
    // 如果没有对应变量：在当前层级 declare + assign
    // 当前没有提供直接在当前层级 declare("variableName", value) 的方法，这样的做法会导致变量不方便跟踪和管理，
    // 推荐的做法：如果要在某个层级声明一个变量，应当在配置上下文中显示声明。即 put 理论上应该仅做 assign 的动作，而不是 declare 的动作。
    public Object put(String name, Object value) {
        // 倒序查找
        // 如果某个层级已存在该变量，则更新；如果所有层级都无该变量，则默认插入最近的变量上下文
        for (int i = contextChain.size() - 1; i >= 0; i--) {
            Context ctx = contextChain.get(i);
            ConfigGroup configGroup = ctx.getConfigGroup();
            if (configGroup == null) continue;
            Map<String, Object> variables = configGroup.getVariableConfigItem();
            if (variables == null) continue;
            if (variables.containsKey(name)) {
                return variables.put(name, value);
            }
        }
        // 上下文链中不存在该变量，默认插入最后一个上下文
        return super.put(name, value);
    }

    public Object remove(String name) {
        //倒序查找
        for (int i = contextChain.size() - 1; i >= 0; i--) {
            Context ctx = contextChain.get(i);
            ConfigGroup configGroup = ctx.getConfigGroup();
            if (configGroup == null) continue;
            Map<String, Object> variables = configGroup.getVariableConfigItem();
            if (variables == null) continue;
            if (variables.containsKey(name)) {
                return variables.remove(name);
            }
        }
        // 上下文链中不存在该变量，直接返回 null
        return null;
    }


}
