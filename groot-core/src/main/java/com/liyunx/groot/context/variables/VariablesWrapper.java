package com.liyunx.groot.context.variables;

import java.util.Map;

/**
 * 变量包装类，尽量不直接使用 Map，减少暴露的 API
 */
public interface VariablesWrapper {

    /**
     * 根据变量名称读取变量值
     *
     * @param name 变量名称
     * @return 变量值
     */
    Object get(String name);

    /**
     * 设置指定名称变量的值
     *
     * @param name  变量名称
     * @param value 变量值
     * @return 变量之前的值，如果有，否则返回 null
     */
    Object put(String name, Object value);

    /**
     * 移除指定变量
     *
     * @param name 变量名称
     * @return 变量之前的值，如果有，否则返回 null
     */
    Object remove(String name);

    /**
     * 合并所有上下文的变量。
     *
     * <p>合并后返回一个新的 Map 对象，对该对象的修改不会反映到上下文的原始变量中。
     *
     * @return 合并后的变量
     */
    Map<String, Object> mergeVariables();

}
