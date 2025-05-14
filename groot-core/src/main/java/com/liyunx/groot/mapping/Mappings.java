package com.liyunx.groot.mapping;

/**
 * 静态辅助类
 */
@SuppressWarnings("unchecked")
public class Mappings {

    public static IntMapping toInt() {
        return IntMapping.INT_MAPPING;
    }

    public static LongMapping toLong() {
        return LongMapping.LONG_MAPPING;
    }

    public static <T> StringMapping<T> toStr() {
        return StringMapping.STRING_MAPPING;
    }

}
