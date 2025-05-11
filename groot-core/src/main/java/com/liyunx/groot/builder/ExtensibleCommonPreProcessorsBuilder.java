package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 额外的公共前置处理器构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共前置处理器。
 */
public abstract class ExtensibleCommonPreProcessorsBuilder<SELF extends ExtensibleCommonPreProcessorsBuilder<SELF>>
    extends AbstractTestElement.PreProcessorsBuilder<SELF> {

    public ExtensibleCommonPreProcessorsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

    // 增加通用的前置处理器（非 Sampler 特有）

}
