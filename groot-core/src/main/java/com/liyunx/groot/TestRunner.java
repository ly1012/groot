package com.liyunx.groot;

import com.liyunx.groot.context.TestContext;

/**
 * 测试用例（含用例驱动数据）执行入口
 *
 * <p>TestRunner 用于运行具备完全相同步骤的一组测试用例，它会在一个线程运行多次，或多个线程运行该用例的拷贝，或使用不同的数据驱动后运行多次，
 * 类似 Jmeter 中的线程组。
 *
 * <p>Groot 中的测试用例，在不特别说明的情况下，是指一个标准测试用例，其与 SessionRunner 一一绑定。
 */
public class TestRunner {

    private final Groot groot;
    private final TestContext testContext;

    TestRunner(Groot groot) {
        this.groot = groot;
        // 没想到有什么需求场景，暂时默认为空
        this.testContext = new TestContext();
    }

    public SessionRunner newSessionRunner() {
        return new SessionRunner(this);
    }

    /**
     * 执行初始化动作
     */
    @Deprecated
    public void start() {
        ApplicationConfig.getTestRunnerListeners().forEach(listener -> listener.testRunnerStart(this));
    }

    /**
     * 执行清理动作
     */
    @Deprecated
    public void stop() {
        ApplicationConfig.getTestRunnerListeners().forEach(listener -> listener.testRunnerStop(this));
    }

    public Groot getGroot() {
        return groot;
    }

    public TestContext getTestContext() {
        return testContext;
    }

}
