package com.liyunx.groot.builder;

import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 所有公共后置处理器的构建
 */
public class CommonPostProcessorsBuilder extends ExtensibleCommonPostProcessorsBuilder<CommonPostProcessorsBuilder, CommonExtractorsBuilder, CommonAssertionsBuilder> {

    public CommonPostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder) {
        super(elementBuilder);
    }

}
