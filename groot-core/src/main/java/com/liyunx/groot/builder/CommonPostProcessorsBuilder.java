package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.AbstractTestElement;
import com.liyunx.groot.testelement.TestResult;

/**
 * 所有公共后置处理器的构建
 */
public class CommonPostProcessorsBuilder<R extends TestResult<R>>
    extends ExtensibleCommonPostProcessorsBuilder<CommonPostProcessorsBuilder<R>, CommonExtractorsBuilder<R>, CommonAssertionsBuilder<R>, R> {

    public CommonPostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder, ContextWrapper ctx) {
        super(elementBuilder, ctx);
    }

}
