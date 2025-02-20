package com.liyunx.groot.context.variables;

import com.liyunx.groot.context.Context;

import java.util.List;

/**
 * SessionRunner 变量包装器
 */
public class SessionVariablesWrapper extends AbstractVariablesWrapper{

    public SessionVariablesWrapper(List<Context> contextChain) {
        super(contextChain);
    }

}
