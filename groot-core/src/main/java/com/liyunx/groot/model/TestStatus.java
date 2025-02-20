package com.liyunx.groot.model;

/**
 * 测试状态，当前仅在前后置处理器结果中用到
 */
public enum TestStatus {

    DISABLED("disabled"),
    PASSED("passed"),
    FAILED("failed"),
    BROKEN("broken"),
    SKIPPED("skipped");

    private final String value;

    TestStatus(String value) {
        this.value = value;
    }

}
