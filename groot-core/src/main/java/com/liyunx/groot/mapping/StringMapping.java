package com.liyunx.groot.mapping;

import com.liyunx.groot.annotation.KeyWord;

@KeyWord("string")
public class StringMapping<T> implements MappingFunction<T, String> {

    @SuppressWarnings("rawtypes")
    public static final StringMapping STRING_MAPPING = new StringMapping();

    @Override
    public String apply(T o) {
        return String.valueOf(o);
    }

}
