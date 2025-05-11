package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.builder.*;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.TestStepContext;
import com.liyunx.groot.testelement.AbstractTestElement;
import com.liyunx.groot.testelement.TestResult;

/**
 * 引用控制器抽象类：执行其他测试元件。
 */
public abstract class AbstractIncludeController<S extends AbstractIncludeController<S, T>, T extends TestResult<T>>
    extends AbstractTestElement<S, T>
    implements Controller<T> {

    public AbstractIncludeController() {
    }

    protected AbstractIncludeController(Builder<S, ?, T> builder) {
        super(builder);
    }

    @Override
    protected TestStepContext createCurrentContext() {
        // 引用步骤默认采用反转覆盖的方案，变量逐级访问和更新都没有问题，即：
        // 1. 被引用测试元件的变量逐级访问优先访问引用步骤的变量
        // 2. 被引用测试元件的变量逐级访问优先更新引用步骤的变量
        // 3. 引用步骤的变量在执行完被引用测试元件后会更新，引用步骤可以访问到该步骤的变量（同步）
        // 变量的跨级访问会跳过反转链直接更新某一层级的变量，如 testcaseVars.put/envVars.put/globalVars.put
        // 这三种情况中仅 testcaseVars.put 的跨级访问会产生不同步问题（引用步骤拿不到跨级访问更新的值），
        // 因此只需对 TestCase Reference 特殊处理即可

        // TODO 是否需要支持测试片段的引用？比如一个或多个测试步骤。
        // 如果是引用多个测试步骤，如何变量覆盖？或者直接不支持覆盖，即不支持对引用片段进行动态传参？
        // 如果为了支持动态传参，在测试片段外再封装一级，不就又回到 TestCase 了吗？
        TestStepContext context = super.createCurrentContext();
        context.setInvert(1);
        return context;
    }

    // ---------------------------------------------------------------------
    // Builder (AbstractIncludeController.Builder)
    // ---------------------------------------------------------------------

    //@formatter:off
    public static abstract class Builder<ELEMENT extends AbstractIncludeController<ELEMENT, R>,
                                         SELF extends AbstractIncludeController.Builder<ELEMENT, SELF, R>,
                                         R extends TestResult<R>>
        extends AbstractTestElement.Builder<ELEMENT, SELF,
                                            AllConfigBuilder,
                                            CommonPreProcessorsBuilder<ELEMENT>,
                                            CommonPostProcessorsBuilder<R>, CommonExtractorsBuilder<R>, CommonAssertionsBuilder<R>>
    //@formatter:on
    {

        // ---------------------------------------------------------------------
        // 获取具体的 Builder 对象
        // ---------------------------------------------------------------------

        @Override
        protected AllConfigBuilder getConfigBuilder() {
            return new AllConfigBuilder();
        }

        @Override
        protected CommonPreProcessorsBuilder<ELEMENT> getSetupBuilder(ContextWrapper ctx) {
            return new CommonPreProcessorsBuilder<>(ctx);
        }

        @Override
        protected CommonPostProcessorsBuilder<R> getTeardownBuilder(ContextWrapper ctx) {
            return new CommonPostProcessorsBuilder<>(this, ctx);
        }

        @Override
        protected CommonExtractorsBuilder<R> getExtractBuilder(ContextWrapper ctx) {
            return new CommonExtractorsBuilder<>(ctx);
        }

        @Override
        protected CommonAssertionsBuilder<R> getAssertBuilder(ContextWrapper ctx) {
            return new CommonAssertionsBuilder<>(ctx);
        }

    }

}
