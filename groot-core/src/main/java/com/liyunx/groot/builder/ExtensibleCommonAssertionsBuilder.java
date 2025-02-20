package com.liyunx.groot.builder;

import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 额外的公共断言构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共断言。
 */
public abstract class ExtensibleCommonAssertionsBuilder<T extends ExtensibleCommonAssertionsBuilder<T>>
    extends AbstractTestElement.AssertionsBuilder<T> {

    // 增加公共的断言处理器（非 Sampler 特有）

}
