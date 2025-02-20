package com.liyunx.groot.context.variables;

import com.liyunx.groot.context.Context;

import java.util.List;

public class LocalVariablesWrapper extends AbstractVariablesWrapper{

    public LocalVariablesWrapper(List<Context> contextChain) {
        super(contextChain);
    }

}
