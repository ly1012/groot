package com.liyunx.groot.context.variables;


import com.liyunx.groot.context.Context;

import java.util.List;

/**
 * TestRunner 变量包装类
 */
public class TestVariablesWrapper extends AbstractVariablesWrapper {

    public TestVariablesWrapper(List<Context> contextChain) {
        super(contextChain);
    }

}
