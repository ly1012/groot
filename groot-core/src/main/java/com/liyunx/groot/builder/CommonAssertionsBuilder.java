package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.TestResult;

/**
 * 所有公共断言的构建
 */
public class CommonAssertionsBuilder<R extends TestResult<R>> extends ExtensibleCommonAssertionsBuilder<CommonAssertionsBuilder<R>, R> {

    public CommonAssertionsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

}
