package com.liyunx.groot.mapping;

import com.liyunx.groot.annotation.KeyWord;

@KeyWord("int")
public class IntMapping implements MappingFunction<String, Integer> {

    public static final IntMapping INT_MAPPING = new IntMapping();

    @Override
    public Integer apply(String input) {
        return Integer.valueOf(input);
    }

}
