package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;

/**
 * 所有公共前置处理器的构建
 */
public class CommonPreProcessorsBuilder extends ExtensibleCommonPreProcessorsBuilder<CommonPreProcessorsBuilder> {

    public CommonPreProcessorsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

}
