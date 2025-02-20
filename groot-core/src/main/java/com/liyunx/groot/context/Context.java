package com.liyunx.groot.context;

import com.liyunx.groot.config.ConfigGroup;

/**
 * 测试上下文接口，通过该接口可以获取每个测试元件的上下文信息
 */
public interface Context {

    /**
     * 获取当前 TestElement 配置组数据。TestElement 配置组数据运行时初始化，属于运行时配置数据。
     *
     * @return TestElement 配置组数据
     */
    ConfigGroup getConfigGroup();

    /**
     * 反转上下文层级，当前仅支持 1 级反转。
     *
     * <p>比如现在有一个测试用例 TestCase1，一个测试用例 TestCase2。TestCase2 中有个测试步骤 TestStep2_1 引用 TestCase1。
     * 那么用例执行时，TestStep2_1 中的 Config 应当覆盖 TestCase1 的 Config，故设置 TestStep2_1 的 revert=1。
     *
     * @return 反转层级
     */
    int getInvert();

}
