package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 额外的公共提取器构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共提取器。
 */
public abstract class ExtensibleCommonExtractorsBuilder<T extends ExtensibleCommonExtractorsBuilder<T>>
    extends AbstractTestElement.ExtractorsBuilder<T> {

    public ExtensibleCommonExtractorsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

    // 增加额外的公共提取处理器（非 Sampler 特有）

}
