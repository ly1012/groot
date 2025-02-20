package com.liyunx.groot.common;

import com.liyunx.groot.context.ContextWrapper;

/**
 * 表示一个对象是可计算的（原地更新），执行 eval 方法，将完成对象中动态数据的计算。
 *
 * @param <T> 可计算的类
 */
public interface Computable<T> {

    /**
     * 计算并替换对象中的动态数据
     *
     * @param ctx 上下文对象
     * @return 计算后的对象（原地更新）
     */
    T eval(ContextWrapper ctx);

}
