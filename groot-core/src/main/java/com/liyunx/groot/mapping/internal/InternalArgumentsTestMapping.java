package com.liyunx.groot.mapping.internal;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.mapping.MappingFunction;

@KeyWord("__internal_arguments_test__")
public class InternalArgumentsTestMapping implements MappingFunction<String, String> {

    private String prefix;
    private String suffix;

    public static InternalArgumentsTestMapping __internal_arguments_test_mapping(String prefix, String suffix) {
        InternalArgumentsTestMapping mapping =  new InternalArgumentsTestMapping();
        mapping.prefix = prefix;
        mapping.suffix = suffix;
        return mapping;
    }

    @Override
    public String apply(String input) {
        return prefix + input + suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
