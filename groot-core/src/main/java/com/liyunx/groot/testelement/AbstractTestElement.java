package com.liyunx.groot.testelement;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.builder.LazyBuilder;
import com.liyunx.groot.builder.TestBuilder;
import com.liyunx.groot.common.Ordered;
import com.liyunx.groot.common.Recoverable;
import com.liyunx.groot.common.Unique;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.config.builtin.FilterConfigItem;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.Context;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.TestStepContext;
import com.liyunx.groot.filter.ExecuteFilterChain;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.processor.HooksPostProcessor;
import com.liyunx.groot.processor.HooksPreProcessor;
import com.liyunx.groot.processor.PostProcessor;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.processor.assertion.Assertion;
import com.liyunx.groot.processor.assertion.standard.EqualToAssertion;
import com.liyunx.groot.processor.extractor.Extractor;
import com.liyunx.groot.processor.extractor.standard.JsonPathExtractor;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.support.Ref;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.liyunx.groot.constants.ExpressionVariable.TEST_RESULT;
import static com.liyunx.groot.constants.TestElementKeyWord.*;
import static java.util.Objects.nonNull;

/**
 * TestElement 抽象实现，提供了共同的属性和逻辑处理
 * <p/>
 * 组装测试元件的通用逻辑（基础属性、配置上下文、前后置）和具体功能逻辑（如控制器的控制逻辑、协议的请求逻辑）
 */
@SuppressWarnings("unchecked")
public abstract class AbstractTestElement<S extends AbstractTestElement<S, T>, T extends TestResult<T>>
    implements TestElement<T>, Recoverable, RunFilterChain, ExecuteFilterChain {

    private static final Logger log = LoggerFactory.getLogger(AbstractTestElement.class);

    public static final String TEST_STEP_NUMBER_STACK = "__TestStep_Number_Stack__";
    public static final String TEST_STEP_NUMBER_PREVIOUS_NO = "__TestStep_Number_PreviousNo__";

    public AbstractTestElement() {
    }

    protected AbstractTestElement(Builder<S, ?, ?, ?, ?, ?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.disabled = builder.disabled;

        this.config = builder.config;

        this.setupBefore = builder.setupBefore;
        this.teardown = builder.teardown;
        this.extract = builder.extract;
        this.assert_ = builder.assert_;

        this.metadata = builder.metadata;
    }

    /* ------------------------------------------------------------ */
    // 声明时数据，不可修改

    @JSONField(name = NAME, ordinal = 1)
    protected String name;

    @JSONField(name = DESCRIPTION, ordinal = 2)
    protected String description;

    @JSONField(name = DISABLED, ordinal = 3)
    protected boolean disabled = false;

    @JSONField(name = CONFIG, ordinal = 4)
    protected TestElementConfig config;

    // 模板解析前执行，Sampler 有额外的前置处理字段，会在模板解析后执行
    @JSONField(name = SETUP_BEFORE, ordinal = 5)
    protected List<PreProcessor> setupBefore = new ArrayList<>();

    @JSONField(name = EXTRACT, ordinal = 7)
    protected List<Extractor> extract;

    @JSONField(name = VALIDATE, ordinal = 8)
    protected List<Assertion> assert_;

    @JSONField(name = TEAR_DOWN, ordinal = 9)
    protected List<PostProcessor> teardown = new ArrayList<>();

    // 元数据，可以挂载一些辅助数据
    @JSONField(name = METADATA)
    protected Map<String, Object> metadata = new HashMap<>();

    // 原始数据（来自文本，如 yaml/json）
    @JSONField(serialize = false, deserialize = false)
    protected String rawTextData;

    /* ------------------------------------------------------------ */
    // 运行时数据，对外开放
    @JSONField(serialize = false, deserialize = false)
    protected S running;

    // 运行时数据，仅内部可用
    protected List<TestFilter> filters;
    private Iterator<TestFilter> runFilters;
    private Iterator<TestFilter> executeFilters;
    protected boolean initialized = false;

    // 测试元件通用逻辑
    @Override
    public final T run(SessionRunner session) {
        SnapshotData snapshotData = new SnapshotData();
        updateTestStepNumber(session, snapshotData);
        recordStartTime(snapshotData);

        if (isStepDisabled(snapshotData)) {
            return snapshotData.testResult;
        }

        // 执行对象初始化动作，仅在当前对象第一次运行 run 方法时调用初始化方法
        if (!initialized) {
            init(session);
            initialized = true;
        }

        // 恢复运行时数据到初始状态（声明时数据），因为同一个元件对象可能被调用多次，每次运行后运行时数据都会变化
        // 比如配置风格用例，一个 Repeat 控制器下执行 3 次 HTTP 请求，每次请求都是基于同一个 HttpSampler 对象
        recover(session);

        ContextWrapper contextWrapper = updateCurrentContextInfo(session, snapshotData);

        // 获取所有符合条件的 TestFilter
        handleFilters(contextWrapper);

        // 执行切面和元件逻辑，包括通用逻辑（前后置处理器等）和具体功能逻辑（流程控制/协议请求等）
        doRun(contextWrapper);

        restoreCurrentContextInfo(session, snapshotData);
        restoreTestStepNumber(session, snapshotData);
        recordEndTime(snapshotData);
        return snapshotData.testResult;
    }

    @Override
    public final void doRun(ContextWrapper ctx) {
        if (runFilters.hasNext()) {
            TestFilter next = runFilters.next();
            next.doRun(ctx, this);
        } else {
            internalRun(ctx);
        }
    }

    @Override
    public final void doExecute(ContextWrapper ctx) {
        if (executeFilters.hasNext()) {
            TestFilter next = executeFilters.next();
            next.doExecute(ctx, this);
        } else {
            execute(ctx, (T) ctx.getTestResult());
        }
    }

    private final class SnapshotData {

        private final String keyword = getSelfKeyword();
        private Integer previousNo;
        private String testStepNo;
        private T testResult;
        List<Context> parentContextChain;
        ContextWrapper previousContextWrapper;

        // 获取当前元件关键字
        private String getSelfKeyword() {
            String keyword;
            try {
                keyword = (String) AbstractTestElement.this.getClass().getField("KEY").get(null);
                return keyword.toUpperCase();
            } catch (Exception e) {
                return AbstractTestElement.this.getClass().getSimpleName();
            }
        }
    }

    private void updateTestStepNumber(SessionRunner session, SnapshotData snapshotData) {
        // 步骤编号
        Map<String, Object> sessionStorage = session.getStorage();
        sessionStorage.putIfAbsent(TEST_STEP_NUMBER_STACK, new Stack<Integer>());
        sessionStorage.putIfAbsent(TEST_STEP_NUMBER_PREVIOUS_NO, 0);
        Stack<Integer> testStepNumberStack = (Stack<Integer>) sessionStorage.get(TEST_STEP_NUMBER_STACK);
        snapshotData.previousNo = (Integer) sessionStorage.get(TEST_STEP_NUMBER_PREVIOUS_NO);
        // 插入当前步骤编号值
        testStepNumberStack.push(snapshotData.previousNo + 1);
        // 子级需要重置上一个编号值
        sessionStorage.put(TEST_STEP_NUMBER_PREVIOUS_NO, 0);
        // 计算当前完整编号，如 1.3.1.2
        snapshotData.testStepNo = generateTestStepNumber(testStepNumberStack);
    }

    private void restoreTestStepNumber(SessionRunner session, SnapshotData snapshotData) {
        Map<String, Object> sessionStorage = session.getStorage();
        Stack<Integer> testStepNumberStack = (Stack<Integer>) sessionStorage.get(TEST_STEP_NUMBER_STACK);
        testStepNumberStack.pop();
        sessionStorage.put(TEST_STEP_NUMBER_PREVIOUS_NO, snapshotData.previousNo + 1);
    }

    protected final String generateTestStepNumber(Stack<Integer> testStepNumberStack) {
        return testStepNumberStack.stream().map(String::valueOf).collect(Collectors.joining("."));
    }

    private void recordStartTime(SnapshotData snapshotData) {
        log.info("{} -> {}({})", snapshotData.testStepNo, name, snapshotData.keyword);
        T res = createTestResult();
        if (res == null) {
            throw new NullPointerException(
                String.format("%s#createTestResult() 返回值为 null，请检查代码", this.getClass().getName()));
        }
        res.start();
        snapshotData.testResult = res;
    }

    private void recordEndTime(SnapshotData snapshotData) {
        snapshotData.testResult.end();
        log.info("{} <- {}{}({})，耗时：{} ms",
            snapshotData.testStepNo,
            name,
            Objects.equals(name, running.name) ? "" : " | " + running.name,
            snapshotData.keyword,
            snapshotData.testResult.getTime());
    }

    private boolean isStepDisabled(SnapshotData snapshotData) {
        if (disabled) {
            log.warn("{} -- {}({}) 被禁用，跳过", snapshotData.testStepNo, name, snapshotData.keyword);
            snapshotData.testResult.end();
            return true;
        }
        return false;
    }

    private ContextWrapper updateCurrentContextInfo(SessionRunner session, SnapshotData snapshotData) {
        // 记录更新前的上下文信息
        List<Context> parentContextChain = session.getContextChain();
        ContextWrapper previousContextWrapper = session.getContextWrapper();
        snapshotData.parentContextChain = parentContextChain;
        snapshotData.previousContextWrapper = previousContextWrapper;

        // 更新当前上下文信息
        List<Context> currentContextChain = getContextChain(parentContextChain);
        session.setContextChain(currentContextChain);
        // 构建上下文包装器，封装本次执行的相关信息
        // 在后续多个方法间传递该对象，使用了方法传参，而不是成员变量，防止该对象在不正确的位置被使用，
        // 另一方面，该对象不是对象状态表示，只是一个临时对象，没有必要使用成员变量
        ContextWrapper contextWrapper = new ContextWrapper(session);
        contextWrapper.setTestElement(this);
        contextWrapper.setTestResult(snapshotData.testResult);
        contextWrapper.getLocalVariablesWrapper().put(TEST_RESULT.getValue(), snapshotData.testResult);
        session.setContextWrapper(contextWrapper);
        return contextWrapper;
    }

    private void restoreCurrentContextInfo(SessionRunner session, SnapshotData snapshotData) {
        session.setContextChain(snapshotData.parentContextChain);
        session.setContextWrapper(snapshotData.previousContextWrapper);
    }

    private void handleFilters(ContextWrapper contextWrapper) {
        filters = contextWrapper.getConfigGroup().get(FilterConfigItem.KEY);
        filters = filterAndSortFilters(filters, contextWrapper);
        runFilters = filters.iterator();
    }

    // 当 TestFilter(implements Unique) 重复时，使用最近优先原则
    private List<TestFilter> filterAndSortFilters(List<TestFilter> filters, ContextWrapper ctx) {
        Map<String, Boolean> uniqueMap = new HashMap<>();

        // 过滤不符合条件的 TestFilter
        List<TestFilter> satisfiedFilters = filters.stream()
            .filter(filter -> filter.match(ctx))
            .collect(Collectors.toList());

        // 过滤 Unique(最近优先原则)
        Collections.reverse(satisfiedFilters);
        List<TestFilter> uniqueFilters = satisfiedFilters.stream()
            .filter(filter -> {
                if (!(filter instanceof Unique)) {
                    return true;
                }
                String uniqueId = ((Unique) filter).uniqueId();
                if (uniqueMap.containsKey(uniqueId)) {
                    return false;
                } else {
                    uniqueMap.put(uniqueId, true);
                    return true;
                }
            })
            .collect(Collectors.toList());
        Collections.reverse(uniqueFilters);

        // 优先级排序
        return uniqueFilters.stream()
            .sorted(Comparator.comparingInt(AbstractTestElement::getTestFilterOrder))
            .collect(Collectors.toList());
    }

    // 获取 TestFilter 优先级大小
    private static int getTestFilterOrder(TestFilter filter) {
        return (filter instanceof Ordered)
            ? ((Ordered) filter).getOrder()
            : Ordered.DEFAULT_PRECEDENCE;
    }

    private void internalRun(ContextWrapper contextWrapper) {
        // 模板计算：当前元件的变量配置项（不会计算父级元件）
        // List/Map/String 类型对象会深度计算，BeanWrapper 类型会解包返回，Supplier 类型会调用 get 返回，其他类型直接返回
        evalVariableConfigItem(contextWrapper);

        // 模板计算：name
        if (running.name != null) {
            running.name = contextWrapper.evalAsString(running.name);
        }

        // 模板计算：description
        if (running.description != null)
            running.description = contextWrapper.evalAsString(running.description);

        // 执行前置动作
        for (PreProcessor preProcessor : running.setupBefore) {
            if (!preProcessor.disabled()) {
                preProcessor.process(contextWrapper);
                continue;
            }
            String name = preProcessor.name();
            log.warn("前置处理器 [{}] 被禁用，跳过", name == null ? preProcessor.getClass().getSimpleName() : name);
        }

        // 执行请求
        executeFilters = filters.iterator();
        doExecute(contextWrapper);

        // 执行后置动作
        mergePostProcessors();
        for (PostProcessor postProcessor : running.teardown) {
            if (!postProcessor.disabled()) {
                postProcessor.process(contextWrapper);
                continue;
            }
            String name = postProcessor.name();
            log.warn("{} [{}] 被禁用，跳过",
                postProcessor instanceof Extractor ? "提取器"
                    : postProcessor instanceof Assertion ? "断言"
                    : "后置处理器",
                name == null ? postProcessor.getClass().getSimpleName() : name);
        }
    }

    private void evalVariableConfigItem(ContextWrapper ctx) {
        VariableConfigItem item;
        if (running.config != null
            && (item = running.config.getVariableConfigItem()) != null) {
            ctx.eval(item);
        }
    }

    /**
     * 合并后置元件
     */
    private void mergePostProcessors() {
        // teardown -> extract -> assert：使用后置处理器先解密请求，然后提取字段值，然后断言
        List<PostProcessor> mergedPostProcessors = new ArrayList<>();
        if (running.teardown != null)
            mergedPostProcessors.addAll(running.teardown);
        if (running.extract != null) {
            mergedPostProcessors.addAll(running.extract);
        }
        if (running.assert_ != null) {
            mergedPostProcessors.addAll(running.assert_);
        }
        running.teardown = mergedPostProcessors;
    }


    // ---------------------------------------------------------------------
    // 子类可能需要重写的方法
    // ---------------------------------------------------------------------

    /**
     * 创建一个 TestResult 对象或其子类对象，如 HttpSampleResult
     *
     * @return 测试执行结果对象
     */
    protected abstract T createTestResult();

    /**
     * 测试元件的功能实现，比如发起 HTTP 请求
     */
    protected abstract void execute(ContextWrapper ctx, T testResult);

    /**
     * 当子类有额外的初始操作时，应该重写该方法。
     * <p>
     * 执行初始化操作，当对象第一次调用 {@link #run} 方法时调用。
     */
    protected void init(SessionRunner session) {
        running = newSelf();
    }

    /**
     * 当子类有额外的恢复操作时，应该重写该方法。
     * <p>
     * 执行恢复操作，恢复运行时数据到初始状态（运行时数据可被 {@link TestFilter} 修改，但只能修改某些字段）。
     * <p>本方法用于解决步骤对象重复运行的问题。
     */
    @Override
    public void recover(SessionRunner session) {
        running.name = name;
        running.description = description;
        running.disabled = disabled;

        if (config != null) {
            TestElementConfig runningConfig = new TestElementConfig();
            config.forEach((key, value) -> {
                // 变量配置，立即计算（计算后合并，表达式的值在本级被确定，本级 copy 后计算）
                if (VariableConfigItem.KEY.equals(key)) {
                    runningConfig.put(key, value.copy());
                }
                // 其他配置，延迟计算（合并后计算，表达式的值在末级被确定，末级 copy 后计算）
                else {
                    runningConfig.put(key, value);
                }
            });
            running.config = runningConfig;
        }

        // listener can add/remove element, but never should be change element directly.
        // An element(such as Extractor or Assertion) object maybe execute many times.
        // Element Implementations should never change their attribute data,
        // if you do that, the next execution may fail and result in unexpected behavior.
        if (setupBefore != null)
            running.setupBefore = new ArrayList<>(setupBefore);
        if (extract != null)
            running.extract = new ArrayList<>(extract);
        if (assert_ != null)
            running.assert_ = new ArrayList<>(assert_);
        if (teardown != null)
            running.teardown = new ArrayList<>(teardown);
        if (metadata != null)
            running.metadata = new HashMap<>(metadata);

        filters = null;
    }

    /**
     * 当子类有额外的声明时数据时，应当重写该方法。
     *
     * @return 满足线程安全最小拷贝的新对象
     */
    @Override
    public S copy() {
        S self = newSelf();
        self.name = name;
        self.description = description;
        self.disabled = disabled;
        self.config = config;
        self.setupBefore = setupBefore;
        self.teardown = teardown;
        self.extract = extract;
        self.assert_ = assert_;
        self.metadata = metadata;
        return self;
    }

    /**
     * 当子类有额外的校验字段时，重写该方法。
     *
     * @return 校验结果
     */
    @Override
    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();

        r.appendDescription("\n当前元件名称：" + name);

        // config 数据校验
        r.append(config);

        // PreProcessor 数据校验
        if (setupBefore != null) {
            for (PreProcessor pre : setupBefore) {
                r.append(pre);
            }
        }

        // PostProcessor 数据校验
        if (extract != null) {
            for (Extractor extractor : extract) {
                r.append(extractor);
            }
        }
        if (assert_ != null) {
            for (Assertion assertion : assert_) {
                r.append(assertion);
            }
        }
        if (teardown != null) {
            for (PostProcessor post : teardown) {
                r.append(post);
            }
        }

        return r;
    }

    /**
     * 当前测试元件的上下文链
     *
     * @param parentContext 父上下文链
     */
    protected List<Context> getContextChain(List<Context> parentContext) {
        List<Context> contextChain = new ArrayList<>();
        contextChain.addAll(parentContext);
        contextChain.add(createCurrentContext());
        return contextChain;
    }

    /**
     * 创建当前测试元件的上下文对象，当子类有额外的需求时重写该方法
     *
     * @return 当前测试元件的上下文对象
     */
    protected TestStepContext createCurrentContext() {
        TestStepContext context = new TestStepContext();
        context.setConfigGroup(running.config);
        return context;
    }

    /**
     * 返回本类的一个新对象，默认使用反射通过无参构造器实例化。
     *
     * @return 本类的空对象
     */
    protected S newSelf() {
        try {
            //noinspection unchecked
            return (S) this.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format("通过反射实例化 %s 失败", this.getClass().getName()), e);
        }
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setConfig(TestElementConfig config) {
        this.config = config;
    }

    public TestElementConfig getConfig() {
        return config;
    }

    public List<PreProcessor> getSetupBefore() {
        return setupBefore;
    }

    public void setSetupBefore(List<PreProcessor> setupBefore) {
        this.setupBefore = setupBefore;
    }

    public List<Extractor> getExtract() {
        return extract;
    }

    public void setExtract(List<Extractor> extract) {
        this.extract = extract;
    }

    public List<Assertion> getAssert_() {
        return assert_;
    }

    public void setAssert_(List<Assertion> assert_) {
        this.assert_ = assert_;
    }

    public List<PostProcessor> getTeardown() {
        return teardown;
    }

    public void setTeardown(List<PostProcessor> teardown) {
        this.teardown = teardown;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setRawTextData(String rawTextData) {
        this.rawTextData = rawTextData;
    }

    public String getRawTextData() {
        return rawTextData;
    }

    public S getRunning() {
        return running;
    }

    public void setRunning(S running) {
        this.running = running;
    }

    /**
     * 测试元件 Builder 基础实现
     */
    //@formatter:off
    public static abstract class Builder<ELEMENT extends AbstractTestElement<ELEMENT, ? extends TestResult<?>>,
                                         SELF extends Builder<ELEMENT, SELF,
                                             CONFIG_BUILDER, SETUP_BUILDER, TEARDOWN_BUILDER, EXTRACT_BUILDER, ASSERT_BUILDER>,
                                         CONFIG_BUILDER extends ConfigBuilder<CONFIG_BUILDER>,
                                         SETUP_BUILDER extends PreProcessorsBuilder<SETUP_BUILDER, ELEMENT>,
                                         TEARDOWN_BUILDER extends PostProcessorsBuilder<TEARDOWN_BUILDER, EXTRACT_BUILDER, ASSERT_BUILDER, ? extends TestResult<?>>,
                                         EXTRACT_BUILDER extends ExtractorsBuilder<EXTRACT_BUILDER, ? extends TestResult<?>>,
                                         ASSERT_BUILDER extends AssertionsBuilder<ASSERT_BUILDER, ? extends TestResult<?>>>
        implements TestElementBuilder<ELEMENT>
    //@formatter:on
    {
        // ------- 基本信息 ------- //
        protected String name;
        protected String description;
        protected boolean disabled;
        protected Map<String, Object> metadata = new HashMap<>();

        // ------- 配置上下文 ------- //
        protected TestElementConfig config;

        // ------- 前后置处理器 ------- //
        protected List<PreProcessor> setupBefore;
        protected List<PostProcessor> teardown;
        protected List<Extractor> extract;
        protected List<Assertion> assert_;

        protected SELF self;

        @SuppressWarnings("unchecked")
        protected Builder() {
            self = (SELF) this;
        }

        // ---------------------------------------------------------------------
        // 获取子类对象
        // ---------------------------------------------------------------------

        // 获取具体的 Builder 对象
        // 虽然也可以通过反射获取具体的泛型类型，然后进行实例化，但有较大性能损耗，这里选择抽象方法来获取具体的泛型对象

        protected abstract CONFIG_BUILDER getConfigBuilder();

        protected abstract SETUP_BUILDER getSetupBuilder(ContextWrapper ctx);

        protected abstract TEARDOWN_BUILDER getTeardownBuilder(ContextWrapper ctx);

        protected abstract EXTRACT_BUILDER getExtractBuilder(ContextWrapper ctx);

        protected abstract ASSERT_BUILDER getAssertBuilder(ContextWrapper ctx);

        protected SETUP_BUILDER getSetupBuilder() {
            return getSetupBuilder(null);
        }

        protected TEARDOWN_BUILDER getTeardownBuilder() {
            return getTeardownBuilder(null);
        }

        protected EXTRACT_BUILDER getExtractBuilder() {
            return getExtractBuilder(null);
        }

        protected ASSERT_BUILDER getAssertBuilder() {
            return getAssertBuilder(null);
        }

        // ---------------------------------------------------------------------
        // 基本信息构建
        // ---------------------------------------------------------------------

        /**
         * 步骤名词
         *
         * @param name 名称
         * @return 当前对象
         */
        public SELF name(String name) {
            this.name = name;
            return self;
        }

        /**
         * 步骤描述信息
         *
         * @param description 描述信息
         * @return 当前对象
         */
        public SELF description(String description) {
            this.description = description;
            return self;
        }

        /**
         * 禁用当前步骤（及其子步骤），执行时会跳过该测试元件
         *
         * @return 当前对象
         */
        public SELF disable() {
            this.disabled = true;
            return self;
        }

        /**
         * 是否禁用当前步骤（及其子步骤）
         *
         * @param disabled true：禁用，false: 启用
         * @return 当前对象
         */
        public SELF disable(boolean disabled) {
            this.disabled = disabled;
            return self;
        }

        // TODO 待补充
        public SELF metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return self;
        }

        public SELF metadata(String name, Object value) {
            metadata.put(name, value);
            return self;
        }

        // ---------------------------------------------------------------------
        // 配置构建
        // ---------------------------------------------------------------------

        /**
         * 配置
         *
         * @param config 配置构建函数
         * @return 当前对象
         */
        public SELF config(Customizer<CONFIG_BUILDER> config) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            config.customize(configBuilder);
            this.config = configBuilder.build();
            return self;
        }

        public SELF config(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "CONFIG_BUILDER") Closure<?> cl) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            GroovySupport.call(cl, configBuilder);
            this.config = configBuilder.build();
            return self;
        }

        /**
         * 配置
         *
         * @param builder 配置 Builder
         * @return 当前对象
         */
        public SELF config(CONFIG_BUILDER builder) {
            this.config = builder.build();
            return self;
        }

        /**
         * 配置
         *
         * @param config 配置
         * @return 当前对象
         */
        public SELF config(TestElementConfig config) {
            this.config = config;
            return self;
        }

        /**
         * 变量配置项（当仅有变量配置时使用）
         *
         * @param variables 变量配置项函数
         * @return 当前对象
         */
        public SELF variables(Customizer<VariableConfigItem.Builder> variables) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            configBuilder.variables(variables);
            this.config = configBuilder.build();
            return self;
        }

        public SELF variables(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = VariableConfigItem.Builder.class) Closure<?> variables) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            configBuilder.variables(variables);
            this.config = configBuilder.build();
            return self;
        }

        /**
         * 变量配置项（当仅有变量配置时使用）
         *
         * @param builder 变量配置项 Builder
         * @return 当前对象
         */
        public SELF variables(VariableConfigItem.Builder builder) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            configBuilder.variables(builder);
            this.config = configBuilder.build();
            return self;
        }

        /**
         * 变量配置项（当仅有变量配置时使用）
         *
         * @param variableConfigItem 变量配置项
         * @return 当前对象
         */
        public SELF variables(VariableConfigItem variableConfigItem) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            configBuilder.variables(variableConfigItem);
            this.config = configBuilder.build();
            return self;
        }

        /**
         * 变量配置项（当仅有变量配置时使用）
         *
         * @param variables 变量配置
         * @return 当前对象
         */
        public SELF variables(Map<String, Object> variables) {
            CONFIG_BUILDER configBuilder = getConfigBuilder();
            configBuilder.variables(variables);
            this.config = configBuilder.build();
            return self;
        }

        // ---------------------------------------------------------------------
        // 前置处理器构建
        // ---------------------------------------------------------------------

        /**
         * 前置处理器(请求计算前)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算前已计算：变量配置项、名称和描述
         *
         * @param setupBefore 前置处理器构建之函数
         * @return 当前对象
         */
        public SELF lazySetupBefore(Customizer<SETUP_BUILDER> setupBefore) {
            SETUP_BUILDER setupBuilder = getSetupBuilder();
            setupBefore.customize(setupBuilder);
            this.setupBefore = setupBuilder.build();
            return self;
        }

        /**
         * 前置处理器(请求计算前)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算前已计算：变量配置项、名称和描述
         *
         * @param cl 前置处理器构建之闭包
         * @return 当前对象
         */
        public SELF lazySetupBefore(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> cl) {
            SETUP_BUILDER builder = getSetupBuilder();
            GroovySupport.call(cl, builder);
            this.setupBefore = builder.build();
            return self;
        }

        public SELF setupBefore(Customizer<SETUP_BUILDER> setupBefore) {
            this.setupBefore = List.of(ctx -> {
                SETUP_BUILDER setupBuilder = getSetupBuilder(ctx);
                setupBefore.customize(setupBuilder);
            });
            return self;
        }

        public SELF setupBefore(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "SETUP_BUILDER") Closure<?> cl) {
            this.setupBefore = List.of(ctx -> {
                SETUP_BUILDER builder = getSetupBuilder(ctx);
                GroovySupport.call(cl, builder);
            });
            return self;
        }

        /**
         * 前置处理器(请求计算前)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算前已计算：变量配置项、名称和描述
         *
         * @param builder 前置处理器 Builder
         * @return 当前测试对象
         */
        public SELF setupBefore(SETUP_BUILDER builder) {
            this.setupBefore = builder.build();
            return self;
        }

        /**
         * 前置处理器(请求计算前)，一个或多个前置处理器，可以有多个同类型前置处理器
         * <p>请求计算前已计算：变量配置项、名称和描述
         *
         * @param list 前置处理器列表
         * @return 当前测试对象
         */
        public SELF setupBefore(List<PreProcessor> list) {
            this.setupBefore = list;
            return self;
        }

        // ---------------------------------------------------------------------
        // 后置处理器构建
        // ---------------------------------------------------------------------

        /**
         * 后置处理器，一个或多个后置处理器，可以有多个同类型后置处理器。
         * <p>
         * 后置处理器包括：提取器、断言及任意后置处理器。
         *
         * @param teardown 后置处理器 Builder
         * @return 当前对象
         */
        public SELF lazyTeardown(Customizer<TEARDOWN_BUILDER> teardown) {
            TEARDOWN_BUILDER teardownBuilder = getTeardownBuilder();
            teardown.customize(teardownBuilder);
            this.teardown = teardownBuilder.build();
            return self;
        }

        public SELF lazyTeardown(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "TEARDOWN_BUILDER") Closure<?> cl) {
            TEARDOWN_BUILDER builder = getTeardownBuilder();
            GroovySupport.call(cl, builder);
            this.teardown = builder.build();
            return self;
        }

        public SELF teardown(Customizer<TEARDOWN_BUILDER> teardown) {
            this.teardown = List.of(ctx -> {
                TEARDOWN_BUILDER teardownBuilder = getTeardownBuilder(ctx);
                teardown.customize(teardownBuilder);
            });
            return self;
        }

        public SELF teardown(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "TEARDOWN_BUILDER") Closure<?> cl) {
            this.teardown = List.of(ctx -> {
                TEARDOWN_BUILDER builder = getTeardownBuilder(ctx);
                GroovySupport.call(cl, builder);
            });
            return self;
        }

        // ---------------------------------------------------------------------
        // 后置处理器构建（提取器）
        // ---------------------------------------------------------------------

        /**
         * 提取器（后置处理器），一个或多个提取器，可以有多个同类型提取器
         *
         * @param extract 提取器 Builder
         * @return 当前对象
         */
        public SELF lazyExtract(Customizer<EXTRACT_BUILDER> extract) {
            EXTRACT_BUILDER extractBuilder = getExtractBuilder();
            extract.customize(extractBuilder);
            this.extract = extractBuilder.build();
            return self;
        }

        public SELF lazyExtract(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "EXTRACT_BUILDER") Closure<?> cl) {
            EXTRACT_BUILDER extractBuilder = getExtractBuilder();
            GroovySupport.call(cl, extractBuilder);
            this.extract = extractBuilder.build();
            return self;
        }

        public SELF extract(Customizer<EXTRACT_BUILDER> extract) {
            this.extract = List.of(ctx -> {
                EXTRACT_BUILDER extractBuilder = getExtractBuilder(ctx);
                extract.customize(extractBuilder);
            });
            return self;
        }

        public SELF extract(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "EXTRACT_BUILDER") Closure<?> cl) {
            this.extract = List.of(ctx -> {
                EXTRACT_BUILDER extractBuilder = getExtractBuilder(ctx);
                GroovySupport.call(cl, extractBuilder);
            });
            return self;
        }

        // ---------------------------------------------------------------------
        // 后置处理器构建（断言）
        // ---------------------------------------------------------------------

        /**
         * 断言（后置处理器），一个或多个断言，可以有多个同类型断言
         *
         * @param assertions 断言 Builder
         * @return 当前对象
         */
        public SELF lazyValidate(Customizer<ASSERT_BUILDER> assertions) {
            ASSERT_BUILDER assertBuilder = getAssertBuilder();
            assertions.customize(assertBuilder);
            this.assert_ = assertBuilder.build();
            return self;
        }

        /**
         * 断言（后置处理器），一个或多个断言，可以有多个同类型断言
         *
         * @param cl 断言闭包
         * @return 当前对象
         */
        public SELF lazyValidate(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "ASSERT_BUILDER") Closure<?> cl) {
            ASSERT_BUILDER assertBuilder = getAssertBuilder();
            GroovySupport.call(cl, assertBuilder);
            this.assert_ = assertBuilder.build();
            return self;
        }

        /**
         * validate 会立即触发断言调用，而 lazyValidate 在最后统一触发断言调用。
         *
         * @see #lazyValidate(Customizer)
         */
        public SELF validate(Customizer<ASSERT_BUILDER> assertions) {
            this.assert_ = List.of(ctx -> {
                ASSERT_BUILDER assertBuilder = getAssertBuilder(ctx);
                assertions.customize(assertBuilder);
            });
            return self;
        }

        /**
         * validate 会立即触发断言调用，而 lazyValidate 在最后统一触发断言调用。
         *
         * @see #lazyValidate(Closure)
         */
        public SELF validate(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "ASSERT_BUILDER") Closure<?> cl) {
            this.assert_ = List.of(ctx -> {
                ASSERT_BUILDER assertBuilder = getAssertBuilder(ctx);
                GroovySupport.call(cl, assertBuilder);
            });
            return self;
        }

    }

    /**
     * 配置构建（包含 core 包中所有公共配置的构建）
     */
    public static abstract class ConfigBuilder<SELF extends ConfigBuilder<SELF>>
        implements TestBuilder<TestElementConfig> {

        protected TestElementConfig config = new TestElementConfig();

        protected SELF self;

        @SuppressWarnings("unchecked")
        protected ConfigBuilder() {
            self = (SELF) this;
        }

        public SELF apply(String key, ConfigItem<?> configItem) {
            config.put(key, configItem);
            return self;
        }

        // ---------------------------------------------------------------------
        // 变量配置
        // ---------------------------------------------------------------------

        /**
         * 变量配置
         *
         * @param variables 变量配置函数
         * @return 当前对象
         */
        public SELF variables(Customizer<VariableConfigItem.Builder> variables) {
            VariableConfigItem.Builder variableConfigItemBuilder = new VariableConfigItem.Builder();
            variables.customize(variableConfigItemBuilder);
            setVariableConfigItem(variableConfigItemBuilder.build());
            return self;
        }

        public SELF variables(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = VariableConfigItem.Builder.class) Closure<?> cl) {
            VariableConfigItem.Builder builder = new VariableConfigItem.Builder();
            GroovySupport.call(cl, builder);
            setVariableConfigItem(builder.build());
            return self;
        }

        /**
         * 变量配置
         *
         * @param builder 变量配置 Builder
         * @return 当前对象
         */
        public SELF variables(VariableConfigItem.Builder builder) {
            setVariableConfigItem(builder.build());
            return self;
        }

        /**
         * 变量配置
         *
         * @param variableConfigItem 变量配置项
         * @return 当前对象
         */
        public SELF variables(VariableConfigItem variableConfigItem) {
            setVariableConfigItem(variableConfigItem);
            return self;
        }

        /**
         * 变量配置
         *
         * @param variables 变量配置
         * @return 当前对象
         */
        public SELF variables(Map<String, Object> variables) {
            setVariableConfigItem(new VariableConfigItem(variables));
            return self;
        }

        // ---------------------------------------------------------------------
        // 插件配置（TestFilter)
        // ---------------------------------------------------------------------

        /**
         * 插件配置（TestFilter)
         *
         * @param filters 插件构建函数
         * @return 当前对象
         */
        public SELF filter(Customizer<FilterConfigItem.Builder> filters) {
            FilterConfigItem.Builder builder = new FilterConfigItem.Builder();
            filters.customize(builder);
            setFilterConfigItem(builder.build());
            return self;
        }

        public SELF filter(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = FilterConfigItem.Builder.class) Closure<?> cl) {
            FilterConfigItem.Builder builder = new FilterConfigItem.Builder();
            GroovySupport.call(cl, builder);
            setFilterConfigItem(builder.build());
            return self;
        }

        private void setVariableConfigItem(VariableConfigItem variableConfigItem) {
            config.put(VariableConfigItem.KEY, variableConfigItem);
        }

        private void setFilterConfigItem(FilterConfigItem filterConfigItem) {
            config.put(FilterConfigItem.KEY, filterConfigItem);
        }

        public TestElementConfig build() {
            return config;
        }

    }

    /**
     * 前置处理器构建（包含 core 包中所有公共前置处理器的构建）
     */
    public static abstract class PreProcessorsBuilder<SELF extends PreProcessorsBuilder<SELF, E>, E>
        implements TestBuilder<List<PreProcessor>> {

        protected final LazyBuilder<PreProcessor> preProcessors = new LazyBuilder<>();

        protected SELF self;
        public final E e;

        @SuppressWarnings("unchecked")
        public PreProcessorsBuilder(ContextWrapper ctx) {
            self = (SELF) this;
            preProcessors.setContextWrapper(ctx);
            if (nonNull(ctx)) {
                e = (E) ctx.getTestElement();
            } else {
                e = null;
            }
        }

        public SELF apply(PreProcessor processor) {
            preProcessors.add(processor);
            return self;
        }

        /**
         * Hook 前置处理器
         *
         * @param hook Hook 字符串，一般为表达式，如 ${log.info('Hello Java!')}
         * @return 当前对象
         */
        public SELF hook(String hook) {
            preProcessors.add(new HooksPreProcessor.Builder().hook(hook).build());
            return self;
        }

        public SELF hooks(Customizer<HooksPreProcessor.Builder> hooks) {
            HooksPreProcessor.Builder builder = new HooksPreProcessor.Builder();
            hooks.customize(builder);
            preProcessors.add(builder.build());
            return self;
        }

        public SELF hooks(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HooksPreProcessor.Builder.class) Closure<?> cl) {
            HooksPreProcessor.Builder builder = new HooksPreProcessor.Builder();
            GroovySupport.call(cl, builder);
            preProcessors.add(builder.build());
            return self;
        }

        public List<PreProcessor> build() {
            return preProcessors;
        }

    }

    /**
     * 后置处理器构建（包含 core 包中所有公共后置处理器的构建）
     */
    //@formatter:off
    public static abstract class PostProcessorsBuilder<SELF extends PostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER, R>,
                                                       EXTRACT_BUILDER extends ExtractorsBuilder<EXTRACT_BUILDER, R>,
                                                       ASSERT_BUILDER extends AssertionsBuilder<ASSERT_BUILDER, R>,
                                                       R extends TestResult<R>>
        implements TestBuilder<List<PostProcessor>>
    //@formatter:on
    {
        protected final LazyBuilder<PostProcessor> postProcessors = new LazyBuilder<>();

        protected AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder;
        protected SELF self;
        public final R r;

        @SuppressWarnings("unchecked")
        protected PostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder, ContextWrapper ctx) {
            this.elementBuilder = elementBuilder;
            self = (SELF) this;
            postProcessors.setContextWrapper(ctx);
            if (nonNull(ctx)) {
                r = (R) ctx.getTestResult();
            } else {
                r = null;
            }
        }

        /**
         * Hook 后置处理器
         *
         * @param hook Hook 字符串，一般为表达式，如 ${log.info('Hello Java!')}
         * @return 当前对象
         */
        public SELF hook(String hook) {
            postProcessors.add(new HooksPostProcessor.Builder().hook(hook).build());
            return self;
        }

        public SELF hooks(Customizer<HooksPostProcessor.Builder> hooks) {
            HooksPostProcessor.Builder builder = new HooksPostProcessor.Builder();
            hooks.customize(builder);
            postProcessors.add(builder.build());
            return self;
        }

        public SELF hooks(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HooksPostProcessor.Builder.class) Closure<?> cl) {
            HooksPostProcessor.Builder builder = new HooksPostProcessor.Builder();
            GroovySupport.call(cl, builder);
            postProcessors.add(builder.build());
            return self;
        }

        public SELF apply(PostProcessor processor) {
            postProcessors.add(processor);
            return self;
        }

        public SELF lazyExtract(Customizer<EXTRACT_BUILDER> extract) {
            EXTRACT_BUILDER builder = (EXTRACT_BUILDER) elementBuilder.getExtractBuilder();
            extract.customize(builder);
            this.postProcessors.addAll(builder.build());
            return self;
        }

        public SELF lazyExtract(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "EXTRACT_BUILDER") Closure<?> cl) {
            EXTRACT_BUILDER builder = (EXTRACT_BUILDER) elementBuilder.getExtractBuilder();
            GroovySupport.call(cl, builder);
            this.postProcessors.addAll(builder.build());
            return self;
        }

        public SELF extract(Customizer<EXTRACT_BUILDER> extract) {
            this.postProcessors.add(ctx -> {
                EXTRACT_BUILDER builder = (EXTRACT_BUILDER) elementBuilder.getExtractBuilder(ctx);
                extract.customize(builder);
            });
            return self;
        }

        public SELF extract(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "EXTRACT_BUILDER") Closure<?> cl) {
            this.postProcessors.add(ctx -> {
                EXTRACT_BUILDER builder = (EXTRACT_BUILDER) elementBuilder.getExtractBuilder(ctx);
                GroovySupport.call(cl, builder);
            });
            return self;
        }

        public SELF lazyValidate(Customizer<ASSERT_BUILDER> validate) {
            ASSERT_BUILDER builder = (ASSERT_BUILDER) elementBuilder.getAssertBuilder();
            validate.customize(builder);
            this.postProcessors.addAll(builder.build());
            return self;
        }

        public SELF lazyValidate(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "ASSERT_BUILDER") Closure<?> cl) {
            ASSERT_BUILDER builder = (ASSERT_BUILDER) elementBuilder.getAssertBuilder();
            GroovySupport.call(cl, builder);
            this.postProcessors.addAll(builder.build());
            return self;
        }

        public SELF validate(Customizer<ASSERT_BUILDER> validate) {
            this.postProcessors.add(ctx -> {
                ASSERT_BUILDER builder = (ASSERT_BUILDER) elementBuilder.getAssertBuilder(ctx);
                validate.customize(builder);
            });
            return self;
        }

        public SELF validate(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, type = "ASSERT_BUILDER") Closure<?> cl) {
            this.postProcessors.add(ctx -> {
                ASSERT_BUILDER builder = (ASSERT_BUILDER) elementBuilder.getAssertBuilder(ctx);
                GroovySupport.call(cl, builder);
            });
            return self;
        }

        public List<PostProcessor> build() {
            return postProcessors;
        }


    }

    /**
     * 后置处理器之提取器构建（包含 core 包中所有公共提取器的构建）
     */
    public static abstract class ExtractorsBuilder<SELF extends ExtractorsBuilder<SELF, R>, R extends TestResult<R>>
        implements TestBuilder<List<Extractor>> {

        protected final LazyBuilder<Extractor> extractors = new LazyBuilder<>();

        protected SELF self;
        public final R r;

        @SuppressWarnings("unchecked")
        public ExtractorsBuilder(ContextWrapper ctx) {
            self = (SELF) this;
            extractors.setContextWrapper(ctx);
            if (nonNull(ctx)) {
                r = (R) ctx.getTestResult();
            } else {
                r = null;
            }
        }

        /**
         * 自定义提取器
         *
         * @param extractor 自定义提取器
         * @return 当前对象
         */
        public SELF apply(Extractor extractor) {
            extractors.add(extractor);
            return self;
        }

        /* ------------------------------------------------------------ */
        // JsonPathExtractor

        public SELF jsonpath(Ref<?> ref, String expression) {
            extractors.add(new JsonPathExtractor.Builder().ref(ref).expression(expression).build());
            return self;
        }

        public SELF jsonpath(Ref<?> ref, String expression, Customizer<JsonPathExtractor.Builder> params) {
            JsonPathExtractor.Builder builder = new JsonPathExtractor.Builder();
            params.customize(builder);
            builder.ref(ref).expression(expression);
            extractors.add(builder.build());
            return self;
        }

        public SELF jsonpath(Ref<?> ref, String expression,
                             @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = JsonPathExtractor.Builder.class) Closure<?> params) {
            JsonPathExtractor.Builder builder = new JsonPathExtractor.Builder();
            GroovySupport.call(params, builder);
            builder.ref(ref).expression(expression);
            extractors.add(builder.build());
            return self;
        }

        /**
         * JsonPath 提取器
         *
         * @param refName    变量名，提取的数据将保存到该变量
         * @param expression JsonPath 表达式
         * @return 当前对象
         */
        public SELF jsonpath(String refName, String expression) {
            extractors.add(new JsonPathExtractor.Builder().refName(refName).expression(expression).build());
            return self;
        }

        public <T> T jsonpath(String expression) {
            Ref<T> ref = Ref.ref();
            extractors.add(new JsonPathExtractor.Builder().ref(ref).expression(expression).build());
            return ref.value;
        }

        public SELF jsonpath(String refName, String expression, Customizer<JsonPathExtractor.Builder> params) {
            JsonPathExtractor.Builder builder = new JsonPathExtractor.Builder();
            params.customize(builder);
            builder.refName(refName).expression(expression);
            extractors.add(builder.build());
            return self;
        }

        public SELF jsonpath(String refName, String expression,
                             @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = JsonPathExtractor.Builder.class) Closure<?> params) {
            JsonPathExtractor.Builder builder = new JsonPathExtractor.Builder();
            GroovySupport.call(params, builder);
            builder.refName(refName).expression(expression);
            extractors.add(builder.build());
            return self;
        }

        public List<Extractor> build() {
            return extractors;
        }

    }

    /**
     * 后置处理器之断言构建（包含 core 包中所有公共断言的构建）
     */
    public static abstract class AssertionsBuilder<SELF extends AssertionsBuilder<SELF, R>, R extends TestResult<R>>
        implements TestBuilder<List<Assertion>> {

        protected final LazyBuilder<Assertion> assertions = new LazyBuilder<>();

        protected SELF self;
        public final R r;

        @SuppressWarnings("unchecked")
        public AssertionsBuilder(ContextWrapper ctx) {
            self = (SELF) this;
            assertions.setContextWrapper(ctx);
            if (nonNull(ctx)) {
                r = (R) ctx.getTestResult();
            } else {
                r = null;
            }
        }

        public SELF apply(Assertion assertion) {
            assertions.add(assertion);
            return self;
        }

        /* ------------------------------------------------------------ */
        // EqualToAssertion

        public SELF equalTo(Object actual, Object expected) {
            equalTo(actual, expected, false);
            return self;
        }

        public SELF equalTo(Object actual, Object expected, boolean ignoreCase) {
            equalTo(actual, expected, it -> it.ignoreCase(ignoreCase));
            return self;
        }

        // equalTo("abc", "ABC", params -> params.ignoreCase())
        public SELF equalTo(Object actual, Object expected, Customizer<EqualToAssertion.Builder> params) {
            EqualToAssertion.Builder builder = new EqualToAssertion.Builder();
            params.customize(builder);
            builder.check(actual).expect(expected);
            assertions.add(builder.build());
            return self;
        }

        // equalTo "abc", "ABC", { ignoreCase() }
        public SELF equalTo(Object actual, Object expected,
                            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = EqualToAssertion.Builder.class) Closure<?> cl) {
            EqualToAssertion.Builder builder = new EqualToAssertion.Builder();
            GroovySupport.call(cl, builder);
            builder.check(actual).expect(expected);
            assertions.add(builder.build());
            return self;
        }

        public List<Assertion> build() {
            return assertions;
        }

    }

}
