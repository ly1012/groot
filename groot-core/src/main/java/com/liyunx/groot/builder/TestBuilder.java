package com.liyunx.groot.builder;

/**
 * Builder 接口
 *
 * @param <T> 对象类型
 */
public interface TestBuilder<T> {

    /**
     * 构建一个对象并返回。
     *
     * @return 一个实例
     */
    T build();

}
