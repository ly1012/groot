package com.liyunx.groot.testelement;

import com.liyunx.groot.builder.TestBuilder;

/**
 * TestElement Builder 接口
 *
 * @param <T> TestElement 类型
 */
public interface TestElementBuilder<T extends TestElement<?>>
    extends TestBuilder<T>
{

    /**
     * 构建一个 TestElement 对象并返回。
     *
     * @return TestElement 实例
     */
    T build();

}
