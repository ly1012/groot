package com.liyunx.groot.builder;

import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 额外的公共后置处理器构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共后置处理器。
 */
public abstract class ExtensibleCommonPostProcessorsBuilder<
    SELF extends ExtensibleCommonPostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER>,
    EXTRACT_BUILDER extends AbstractTestElement.ExtractorsBuilder<EXTRACT_BUILDER>,
    ASSERT_BUILDER extends AbstractTestElement.AssertionsBuilder<ASSERT_BUILDER>>
    extends AbstractTestElement.PostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER> {

    public ExtensibleCommonPostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder) {
        super(elementBuilder);
    }

    // 增加额外的公共后置处理器（非 Sampler 特有）

}
