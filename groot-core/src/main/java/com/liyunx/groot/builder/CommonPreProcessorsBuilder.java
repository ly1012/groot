package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 所有公共前置处理器的构建
 */
public class CommonPreProcessorsBuilder<E extends AbstractTestElement<E, ?>> extends ExtensibleCommonPreProcessorsBuilder<CommonPreProcessorsBuilder<E>, E> {

    public CommonPreProcessorsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

}
