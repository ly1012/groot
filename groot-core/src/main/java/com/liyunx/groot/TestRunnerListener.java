package com.liyunx.groot;

@Deprecated
public interface TestRunnerListener {

    /**
     * TestRunner 开始时调用，执行初始化动作
     */
    void testRunnerStart(TestRunner testRunner);

    /**
     * TestRunner 结束时调用，执行清理动作
     */
    void testRunnerStop(TestRunner testRunner);

}
