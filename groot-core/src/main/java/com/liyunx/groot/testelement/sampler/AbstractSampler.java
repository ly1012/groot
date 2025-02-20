package com.liyunx.groot.testelement.sampler;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.builder.*;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.SampleFilterChain;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.testelement.AbstractTestElement;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.testelement.TestElementBuilder;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.liyunx.groot.constants.TestElementKeyWord.SETUP_AFTER;

/**
 * Sampler 抽象实现类。
 *
 * <p>扩展 Sampler 请继承此类。
 */
public abstract class AbstractSampler<S extends AbstractSampler<S, T>, T extends SampleResult<T>>
    extends AbstractTestElement<S, T>
    implements Sampler<T>, SampleFilterChain {

    @JSONField(name = SETUP_AFTER, ordinal = 5)
    protected List<PreProcessor> setupAfter = new ArrayList<>();

    private Iterator<TestFilter> sampleFilters;

    public AbstractSampler() {
    }

    protected AbstractSampler(Builder<S, ?, ?, ?, ?, ?, ?> builder) {
        super(builder);
        this.setupAfter = builder.setupAfter;
    }

    // ---------------------------------------------------------------------
    // 重写 AbstractTestElement 中的方法
    // ---------------------------------------------------------------------

    @Override
    protected void execute(ContextWrapper ctx, T result) {
        handleRequest(ctx, result);

        // 执行前置动作（请求计算后）
        for (PreProcessor preProcessor : running.setupAfter) {
            preProcessor.process(ctx);
        }

        // 子类可以在 sample 方法或其子方法内，在合适的时机再次调用 sampleStart 和 sampleEnd 方法，
        // 以获取更准确的 sample 时间
        result.sampleStart();
        sampleFilters = filters.iterator();
        doSample(ctx);
        if (result.getSampleEndTime() == 0)
            result.sampleEnd();

        handleResponse(ctx, result);
    }

    // ---------------------------------------------------------------------
    // 实现 SampleFilterChain 中的方法
    // ---------------------------------------------------------------------

    @Override
    public final void doSample(ContextWrapper ctx) {
        if (sampleFilters.hasNext()) {
            TestFilter next = sampleFilters.next();
            next.doSample(ctx, this);
        } else {
            sample(ctx, (T) ctx.getTestResult());
        }
    }

    // ---------------------------------------------------------------------
    // 子类可能需要重写的方法
    // ---------------------------------------------------------------------

    @Override
    public void recover(SessionRunner session) {
        super.recover(session);
        if (setupAfter != null) {
            running.setupAfter = new ArrayList<>(setupAfter);
        }
    }

    @Override
    public S copy() {
        S self = super.copy();
        self.setupAfter = setupAfter;
        return self;
    }

    /**
     * 请求执行前处理。比如请求数据的表达式计算。
     *
     * <p>该方法在 {@link TestFilter#doSample} 之前调用。
     */
    protected void handleRequest(ContextWrapper contextWrapper, T result) {
        // do nothing.
    }

    /**
     * 执行请求。
     * <p>不要在该方法内进行请求的动态数据替换，请使用 {@link AbstractSampler#handleRequest(ContextWrapper, SampleResult)}。
     */
    protected abstract void sample(ContextWrapper contextWrapper, T result);

    /**
     * 请求执行后处理。
     *
     * <p>该方法在 {@link TestFilter#doSample} 之后调用。
     */
    protected void handleResponse(ContextWrapper contextWrapper, T result) {
        // do nothing.
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public List<PreProcessor> getSetupAfter() {
        return setupAfter;
    }

    public void setSetupAfter(List<PreProcessor> setupAfter) {
        this.setupAfter = setupAfter;
    }

    // ---------------------------------------------------------------------
    // Builder (AbstractSampler.Builder)
    // ---------------------------------------------------------------------

    /**
     * Sampler Builder 基础实现
     */
    //@formatter:off
    public static abstract class Builder<ELEMENT extends AbstractSampler<ELEMENT, ?>,
                                         SELF extends AbstractSampler.Builder<ELEMENT, SELF,
                                                                              CONFIG_BUILDER,
                                                                              SETUP_BUILDER, TEARDOWN_BUILDER, EXTRACT_BUILDER, ASSERT_BUILDER>,
                                         CONFIG_BUILDER extends ExtensibleCommonConfigBuilder<CONFIG_BUILDER>,
                                         SETUP_BUILDER extends ExtensibleCommonPreProcessorsBuilder<SETUP_BUILDER>,
                                         TEARDOWN_BUILDER extends ExtensibleCommonPostProcessorsBuilder<TEARDOWN_BUILDER, EXTRACT_BUILDER, ASSERT_BUILDER>,
                                         EXTRACT_BUILDER extends ExtensibleCommonExtractorsBuilder<EXTRACT_BUILDER>,
                                         ASSERT_BUILDER extends ExtensibleCommonAssertionsBuilder<ASSERT_BUILDER>>
        extends AbstractTestElement.Builder<ELEMENT, SELF,
                                            CONFIG_BUILDER,
                                            SETUP_BUILDER, TEARDOWN_BUILDER, EXTRACT_BUILDER, ASSERT_BUILDER>
        implements TestElementBuilder<ELEMENT>
    //@formatter:on
    {
        protected List<PreProcessor> setupAfter;

        // ---------------------------------------------------------------------
        // 前置处理器(解析后)构建
        // ---------------------------------------------------------------------

        /**
         * 前置处理器(请求计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算后已计算所有动态数据，包括：变量配置项、名称、描述、协议请求内容
         *
         * @param setup 前置处理器构建函数
         * @return 当前对象
         */
        public SELF setupAfter(Customizer<SETUP_BUILDER> setup) {
            SETUP_BUILDER builder = getSetupBuilder();
            setup.customize(builder);
            this.setupAfter = builder.build();
            return self;
        }

        /**
         * 前置处理器(请求计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算后已计算所有动态数据，包括：变量配置项、名称、描述、协议请求内容
         *
         * @param cl 前置处理器构建之闭包
         * @return 当前对象
         */
        public SELF setupAfter(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> cl) {
            SETUP_BUILDER builder = getSetupBuilder();
            GroovySupport.call(cl, builder);
            this.setupAfter = builder.build();
            return self;
        }

        /**
         * 前置处理器(请求计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算后已计算所有动态数据，包括：变量配置项、名称、描述、协议请求内容
         *
         * @param builder 前置处理器 Builder
         * @return 当前测试对象
         */
        public SELF setupAfter(SETUP_BUILDER builder) {
            this.setupAfter = builder.build();
            return self;
        }

        /**
         * 前置处理器(请求计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算后已计算所有动态数据，包括：变量配置项、名称、描述、协议请求内容
         *
         * @param list 前置处理器列表
         * @return 当前测试对象
         */
        public SELF setupAfter(List<PreProcessor> list) {
            this.setupAfter = list;
            return self;
        }

        /**
         * 前置处理器(请求计算前或计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         *
         * @param setup 前置处理器构建之函数
         * @return 当前对象
         */
        public SELF setup(Customizer<SetupBeforeAndAfterBuilder> setup) {
            SetupBeforeAndAfterBuilder builder = new SetupBeforeAndAfterBuilder();
            setup.customize(builder);
            return self;
        }

        /**
         * 前置处理器(请求计算前或计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         *
         * @param cl 前置处理器构建之闭包
         * @return 当前对象
         */
        public SELF setup(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = SetupBeforeAndAfterBuilder.class) Closure<?> cl) {
            SetupBeforeAndAfterBuilder builder = new SetupBeforeAndAfterBuilder();
            GroovySupport.call(cl, builder);
            return self;
        }

        /**
         * 前置处理器(请求计算前或计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
         *
         * @param before 前置处理器构建之闭包(请求计算前)
         * @param after  前置处理器构建之闭包(请求计算后)
         * @return 当前对象
         */
        public SELF setup(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> before,
                          @DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> after) {
            setupBefore(before);
            setupAfter(after);
            return self;
        }

        /**
         * setupBefore 和 setupAfter 组合
         */
        public class SetupBeforeAndAfterBuilder {

            /**
             * 前置处理器(请求计算前)，一个或多个前置处理器，可以有多个同类型前置处理器
             * <p>请求计算前已计算：变量配置项、名称和描述
             *
             * @param before 前置处理器构建之函数
             * @return 当前对象
             */
            public SetupBeforeAndAfterBuilder before(Customizer<SETUP_BUILDER> before) {
                setupBefore(before);
                return this;
            }

            /**
             * 前置处理器(请求计算前)，一个或多个前置处理器，可以有多个同类型前置处理器
             * <p>请求计算前已计算：变量配置项、名称和描述
             *
             * <p><b>warning: </b>
             * Intellij IDEA 无法识别内部类中使用的外部类泛型，即无法识别委托对象类型，
             * 故无法给出方法提示和自动补全(属于 IDE 功能)，但执行正常(正常编译执行)。
             * <br/>暂时没找到好的解决方法，推荐使用 {@link #setup(Closure, Closure)}
             * 或直接使用 {@link #setupBefore(Closure)} 和 {@link #setupAfter(Closure)} 代替。
             *
             * @param cl 前置处理器构建之闭包
             * @return 当前对象
             */
            public SetupBeforeAndAfterBuilder before(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> cl) {
                setupBefore(cl);
                return this;
            }

            /**
             * 前置处理器(请求计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
             * <p>请求计算后已计算所有动态数据，包括：变量配置项、名称、描述、协议请求内容
             *
             * @param after 前置处理器构建之函数
             * @return 当前对象
             */
            public SetupBeforeAndAfterBuilder after(Customizer<SETUP_BUILDER> after) {
                setupAfter(after);
                return this;
            }

            /**
             * 前置处理器(请求计算后)，一个或多个前置处理器，可以有多个同类型前置处理器
             * <p>请求计算后已计算所有动态数据，包括：变量配置项、名称、描述、协议请求内容
             *
             * <p><b>warning: </b>
             * Intellij IDEA 无法识别内部类中使用的外部类泛型，即无法识别委托对象类型，
             * 故无法给出方法提示和自动补全(属于 IDE 功能)，但执行正常(正常编译执行)。
             * 暂时没找到好的解决方法，推荐使用 {@link #setup(Closure, Closure)}
             * 或直接使用 {@link #setupBefore(Closure)} 和 {@link #setupAfter(Closure)} 代替。
             *
             * @param cl 前置处理器构建之闭包
             * @return 当前对象
             */
            public SetupBeforeAndAfterBuilder after(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> cl) {
                setupAfter(cl);
                return this;
            }

        }

    }

}
