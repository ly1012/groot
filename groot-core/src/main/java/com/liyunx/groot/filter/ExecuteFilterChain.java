package com.liyunx.groot.filter;

import com.liyunx.groot.context.ContextWrapper;

public interface ExecuteFilterChain {

    void doExecute(ContextWrapper ctx);

}
