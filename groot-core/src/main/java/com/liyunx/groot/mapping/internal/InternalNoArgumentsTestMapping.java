package com.liyunx.groot.mapping.internal;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.mapping.MappingFunction;

@KeyWord("__internal_no_arguments_test__")
public class InternalNoArgumentsTestMapping implements MappingFunction<String, String> {

    public static final InternalNoArgumentsTestMapping __INTERNAL_NO_ARGUMENTS_TEST_MAPPING = new InternalNoArgumentsTestMapping();

    @Override
    public String apply(String input) {
        return "<<<" + input + ">>>";
    }

}
