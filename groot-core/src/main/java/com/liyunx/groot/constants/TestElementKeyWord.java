package com.liyunx.groot.constants;

/**
 * TestElement 常用关键字常量
 */
public final class TestElementKeyWord {

    private TestElementKeyWord() {}

    // 标准 Key
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String DISABLED = "disabled";
    public static final String CONFIG = "config";
    public static final String SETUP_BEFORE = "setupBefore";
    public static final String SETUP_AFTER = "setupAfter";
    public static final String TEAR_DOWN = "teardown";
    public static final String EXTRACT = "extract";
    public static final String VALIDATE = "validate";
    public static final String METADATA = "metadata";

    // 简写 Key
    public static final String SETUP = "setup";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    // KEY 辅助分隔符
    public static final String SEPARATOR = "$";

    // TEAR DOWN 相关 JSON Key
    public static final String TEAR_DOWN_TYPE = "type";             // TEAR_DOWN.[*].type

}
