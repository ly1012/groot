package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.AbstractTestElement;
import com.liyunx.groot.testelement.TestResult;

/**
 * 额外的公共后置处理器构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共后置处理器。
 */
public abstract class ExtensibleCommonPostProcessorsBuilder<
    SELF extends ExtensibleCommonPostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER, R>,
    EXTRACT_BUILDER extends AbstractTestElement.ExtractorsBuilder<EXTRACT_BUILDER, R>,
    ASSERT_BUILDER extends AbstractTestElement.AssertionsBuilder<ASSERT_BUILDER, R>,
    R extends TestResult<R>>
    extends AbstractTestElement.PostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER, R> {

    public ExtensibleCommonPostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder, ContextWrapper ctx) {
        super(elementBuilder, ctx);
    }

    // 增加额外的公共后置处理器（非 Sampler 特有）

}
