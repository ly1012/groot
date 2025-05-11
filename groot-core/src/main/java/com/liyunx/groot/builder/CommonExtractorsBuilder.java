package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.TestResult;

/**
 * 所有公共提取器的构建
 */
public class CommonExtractorsBuilder<R extends TestResult<R>> extends ExtensibleCommonExtractorsBuilder<CommonExtractorsBuilder<R>, R> {

    public CommonExtractorsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

}
