package com.liyunx.groot.constants;

/**
 * 表达式中使用的变量名常量
 */
public enum ExpressionVariable {

    /**
     * 测试元件执行结果变量
     *
     * <p><pre>
     *   ${r.response.status}
     * </pre>
     */
    TEST_RESULT("r"),

    /**
     * 日志对象
     *
     * <p><pre>
     *   ${log.info('更新后：{}', tVars.v3)}
     * </pre>
     */
    LOG("log"),

    /**
     * 上下文包装器 ContextWrapper 对象
     *
     * <p><pre>
     *   String uuid = ctx.evalAsString("${uuid()}")
     * </pre>
     */
    CONTEXT_WRAPPER("ctx"),

    /**
     * SessionRunner 对象
     */
    SESSION_RUNNER("runner"),

    GLOBAL_VARIABLES_WRAPPER("gVars"),
    ENVIRONMENT_VARIABLES_WRAPPER("eVars"),
    TEST_VARIABLES_WRAPPER("tVars"),            // TestRunner
    SESSION_VARIABLES_WRAPPER("sVars"),         // SessionRunner
    LOCAL_VARIABLES_WRAPPER("lVars"),
    ALL_VARIABLES_WRAPPER("vars");

    private final String value;

    ExpressionVariable(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
