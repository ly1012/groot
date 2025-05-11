package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.AbstractTestElement;
import com.liyunx.groot.testelement.TestResult;

/**
 * 额外的公共断言构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共断言。
 */
public abstract class ExtensibleCommonAssertionsBuilder<T extends ExtensibleCommonAssertionsBuilder<T, R>, R extends TestResult<R>>
    extends AbstractTestElement.AssertionsBuilder<T, R> {

    public ExtensibleCommonAssertionsBuilder(ContextWrapper ctx) {
        super(ctx);
    }

    // 增加公共的断言处理器（非 Sampler 特有）

}
