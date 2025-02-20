package com.liyunx.groot.mapping;

import com.liyunx.groot.annotation.KeyWord;

@KeyWord("long")
public class LongMapping implements MappingFunction<String, Long> {

    public static final LongMapping LONG_MAPPING = new LongMapping();

    @Override
    public Long apply(String input) {
        return Long.valueOf(input);
    }

}
