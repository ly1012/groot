package com.liyunx.groot.processor.extractor;

/**
 * 提取时变量的目标作用域，SESSION 表示提取时变量会当作 session 变量处理，默认为 ALL
 */
public enum ExtractScope {

    GLOBAL("global"),
    ENVIRONMENT("environment"),
    TEST("test"),
    SESSION("session"),
    LOCAL("local"),
    ALL("all");

    private final String name;

    ExtractScope(String name) {
        this.name = name;
    }
}
