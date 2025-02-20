package com.liyunx.groot.context.variables;


import com.liyunx.groot.context.Context;

import java.util.List;

/**
 * 全局变量包装类
 */
public class GlobalVariablesWrapper extends AbstractVariablesWrapper {

    public GlobalVariablesWrapper(List<Context> contextChain) {
        super(contextChain);
    }

}
