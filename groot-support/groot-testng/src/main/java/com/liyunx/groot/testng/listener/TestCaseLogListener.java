package com.liyunx.groot.testng.listener;

import org.slf4j.MDC;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Trace ID 生成与清理: 在测试用例开始前生成并设置 Trace ID，在测试用例结束后清理 Trace ID。
 */
public class TestCaseLogListener implements ITestListener {

    private static final String KEY = "testcaseId";

    @Override
    public void onTestStart(ITestResult result) {
        setTraceID(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        removeTraceID();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        removeTraceID();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        removeTraceID();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        removeTraceID();
    }

    @Override
    public void onStart(ITestContext context) {

    }

    @Override
    public void onFinish(ITestContext context) {

    }

    private void setTraceID(ITestResult result) {
        MDC.put(KEY, result.getInstanceName() + "." + result.getName() + "." + result.getMethod().getCurrentInvocationCount());
    }

    private void removeTraceID() {
        if (MDC.get(KEY) != null) {
            MDC.remove(KEY);
        }
    }

}
