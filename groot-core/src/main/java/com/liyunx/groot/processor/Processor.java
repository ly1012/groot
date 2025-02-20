package com.liyunx.groot.processor;

/**
 * 处理器通用方法
 */
public interface Processor {

    /**
     * 是否禁用
     *
     * @return true 表示禁用，默认不禁用
     */
    default boolean disabled() {
        return false;
    }

    /**
     * 处理器名称，一般为动作意图简述，如 "提取订单号" 或 "断言状态码为 200"
     *
     * <p>未显式指定 {@link ProcessResult#getName()} 时的默认值
     *
     * @return 处理器名称
     */
    default String name() {
        return null;
    }

    /**
     * 描述信息，描述处理器做了什么
     *
     * <p>未显式指定 {@link ProcessResult#getDescription()} 时的默认值
     *
     * @return 描述信息
     */
    default String description() {
        return null;
    }

}
