package com.liyunx.groot.mapping;

public class Mappings {

    public static IntMapping toInt() {
        return IntMapping.INT_MAPPING;
    }

    public static LongMapping toLong() {
        return LongMapping.LONG_MAPPING;
    }

    @SuppressWarnings("unchecked")
    public static <T> StringMapping<T> toStr() {
        return StringMapping.STRING_MAPPING;
    }

}
