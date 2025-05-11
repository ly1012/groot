package com.liyunx.groot;

import com.liyunx.groot.builder.AllConfigBuilder;
import com.liyunx.groot.context.variables.LocalVariablesWrapper;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.support.Worker;
import com.liyunx.groot.testelement.*;
import com.liyunx.groot.testelement.controller.*;
import com.liyunx.groot.testelement.sampler.DefaultSampleResult;
import com.liyunx.groot.testelement.sampler.NoopSampler;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.List;
import java.util.Map;

import static com.liyunx.groot.SessionRunner.getSession;
import static groovy.lang.Closure.DELEGATE_ONLY;

/**
 * <p>方法命名规范：
 * <ul>
 *     <li>1. 方法名为关键字或变形关键字：不包含配置和前后置，如 repeat / onWhile / http</li>
 *     <li>2. 方法名为关键字或变形关键字 + With：包含配置和前后置，如 repeatWith / onWhileWith / httpWith</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public class DefaultVirtualRunner {

    /* ------------------------------------------------------------ */
    // SessionRunner

    /**
     * Session 上下文配置
     */
    public static void sessionConfig(Customizer<AllConfigBuilder> config) {
        AllConfigBuilder testElementConfigBuilder = new AllConfigBuilder();
        config.customize(testElementConfigBuilder);
        getSession().config(testElementConfigBuilder.build());
    }

    /**
     * Session 上下文配置
     */
    public static void sessionConfig(@DelegatesTo(strategy = DELEGATE_ONLY, value = AllConfigBuilder.class) Closure<?> cl) {
        AllConfigBuilder testElementConfigBuilder = new AllConfigBuilder();
        GroovySupport.call(cl, testElementConfigBuilder);
        getSession().config(testElementConfigBuilder.build());
    }

    /* ------------------------------------------------------------ */
    // 变量增删改查

    /**
     * 逐级向上查找，一旦找到，则更新该变量的值，否则在当前层级新增该变量。
     *
     * @param name  变量名
     * @param value 变量值
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T v(String name, Object value) {
        return (T) getSession().getContextWrapper().getAllVariablesWrapper().put(name, value);
    }

    /**
     * 逐级向上查找，一旦找到，则返回该变量的值。
     *
     * @param name 变量名
     * @return 变量当前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T v(String name) {
        return (T) getSession().getContextWrapper().getAllVariablesWrapper().get(name);
    }

    /**
     * 逐级向上查找，一旦找到，则删除该变量。
     *
     * @param name 变量名
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T rmv(String name) {
        return (T) getSession().getContextWrapper().getAllVariablesWrapper().remove(name);
    }

    /**
     * 设置局部变量
     *
     * @param name  变量名
     * @param value 变量值
     * @param <T>   变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     * @see LocalVariablesWrapper
     */
    public static <T> T lv(String name, Object value) {
        return (T) getSession().getContextWrapper().getLocalVariablesWrapper().put(name, value);
    }

    /**
     * 查询局部变量
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量当前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T lv(String name) {
        return (T) getSession().getContextWrapper().getLocalVariablesWrapper().get(name);
    }

    /**
     * 删除局部变量
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T rmlv(String name) {
        return (T) getSession().getContextWrapper().getLocalVariablesWrapper().remove(name);
    }

    /**
     * 设置会话变量（SessionRunner 级别）
     *
     * @param name  变量名
     * @param value 变量值
     * @param <T>   变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T sv(String name, Object value) {
        return (T) getSession().getContextWrapper().getSessionVariablesWrapper().put(name, value);
    }

    /**
     * 查询会话变量（SessionRunner 级别）
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量当前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T sv(String name) {
        return (T) getSession().getContextWrapper().getSessionVariablesWrapper().get(name);
    }

    /**
     * 删除会话变量（SessionRunner 级别）
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T rmsv(String name) {
        return (T) getSession().getContextWrapper().getSessionVariablesWrapper().remove(name);
    }

    /**
     * 设置测试变量（TestRunner 级别）
     *
     * @param name  变量名
     * @param value 变量值
     * @param <T>   变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T tv(String name, Object value) {
        return (T) getSession().getContextWrapper().getTestVariablesWrapper().put(name, value);
    }

    /**
     * 查询测试变量（TestRunner 级别）
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量当前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T tv(String name) {
        return (T) getSession().getContextWrapper().getTestVariablesWrapper().get(name);
    }

    /**
     * 删除测试变量（TestRunner 级别）
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T rmtv(String name) {
        return (T) getSession().getContextWrapper().getTestVariablesWrapper().remove(name);
    }

    /**
     * 设置环境变量
     *
     * @param name  变量名
     * @param value 变量值
     * @param <T>   变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T ev(String name, Object value) {
        return (T) getSession().getContextWrapper().getEnvironmentVariablesWrapper().put(name, value);
    }

    /**
     * 查询环境变量
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量当前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T ev(String name) {
        return (T) getSession().getContextWrapper().getEnvironmentVariablesWrapper().get(name);
    }

    /**
     * 删除环境变量
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T rmev(String name) {
        return (T) getSession().getContextWrapper().getEnvironmentVariablesWrapper().remove(name);
    }

    /**
     * 设置全局变量
     *
     * @param name  变量名
     * @param value 变量值
     * @param <T>   变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T gv(String name, Object value) {
        return (T) getSession().getContextWrapper().getGlobalVariablesWrapper().put(name, value);
    }

    /**
     * 查询全局变量
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量当前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T gv(String name) {
        return (T) getSession().getContextWrapper().getGlobalVariablesWrapper().get(name);
    }

    /**
     * 删除全局变量
     *
     * @param name 变量名
     * @param <T>  变量值类型
     * @return 变量之前的值，如果不存在对应的变量则返回 null
     */
    public static <T> T rmgv(String name) {
        return (T) getSession().getContextWrapper().getGlobalVariablesWrapper().remove(name);
    }

    /* ------------------------------------------------------------ */
    // 任意元件

    public static <T extends TestResult<T>> T step(String name, TestElement<T> testElement) {
        if (testElement instanceof AbstractTestElement<?, ?>) {
            ((AbstractTestElement<?, ?>) testElement).setName(name);
        }
        return getSession().run(testElement);
    }

    /* ------------------------------------------------------------ */
    // ForEachController

    public static DefaultTestResult foreach(String name, String file, Worker steps) {
        ForEachController.Builder builder = new ForEachController.Builder();
        builder.name(name);
        builder.forSettings(forSettings -> forSettings.file(file));
        builder.step(new BridgeTestElement(steps));
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreach(String name, List<Map<String, Object>> data, Worker steps) {
        ForEachController.Builder builder = new ForEachController.Builder();
        builder.name(name);
        builder.forSettings(forSettings -> forSettings.data(data));
        builder.step(new BridgeTestElement(steps));
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreach(String name,
                                            Customizer<ForEachController.ForSettings.Builder> forSettings,
                                            Worker steps) {
        ForEachController.Builder builder = new ForEachController.Builder();
        builder.name(name);
        builder.forSettings(forSettings);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreach(String name,
                                            @DelegatesTo(strategy = DELEGATE_ONLY, value = ForEachController.ForSettings.Builder.class) Closure<?> cl,
                                            Worker steps) {
        ForEachController.Builder builder = new ForEachController.Builder();
        builder.name(name);
        builder.forSettings(cl);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreachWith(String name,
                                                Customizer<ForEachController.Builder> it,
                                                Worker steps) {
        ForEachController.Builder builder = new ForEachController.Builder();
        it.customize(builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreachWith(String name,
                                                @DelegatesTo(strategy = DELEGATE_ONLY, value = ForEachController.Builder.class) Closure<?> cl,
                                                Worker steps) {
        ForEachController.Builder builder = new ForEachController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreachWith(String name,
                                                Customizer<ForEachController.Builder> it) {
        ForEachController.Builder builder = new ForEachController.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult foreachWith(String name,
                                                @DelegatesTo(strategy = DELEGATE_ONLY, value = ForEachController.Builder.class) Closure<?> cl) {
        ForEachController.Builder builder = new ForEachController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // RepeatController

    public static DefaultTestResult repeat(String name, int times, Worker steps) {
        RepeatController.Builder builder = new RepeatController.Builder();
        builder.name(name);
        builder.times(times);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult repeat(String name, String times, Worker steps) {
        RepeatController.Builder builder = new RepeatController.Builder();
        builder.name(name);
        builder.times(times);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult repeatWith(String name,
                                               Customizer<RepeatController.Builder> it,
                                               Worker steps) {
        RepeatController.Builder builder = new RepeatController.Builder();
        it.customize(builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult repeatWith(String name,
                                               @DelegatesTo(strategy = DELEGATE_ONLY, value = RepeatController.Builder.class) Closure<?> cl,
                                               Worker steps) {
        RepeatController.Builder builder = new RepeatController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult repeatWith(String name,
                                               Customizer<RepeatController.Builder> it) {
        RepeatController.Builder builder = new RepeatController.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult repeatWith(String name,
                                               @DelegatesTo(strategy = DELEGATE_ONLY, value = RepeatController.Builder.class) Closure<?> cl) {
        RepeatController.Builder builder = new RepeatController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // WhileController

    public static DefaultTestResult onWhile(String name,
                                            String condition,
                                            Worker steps) {
        WhileController.Builder builder = new WhileController.Builder();
        builder.name(name);
        builder.whileSettings(whileSettings -> whileSettings.condition(condition));
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onWhile(String name,
                                            Customizer<WhileController.WhileSettings.Builder> whileSettings,
                                            Worker steps) {
        WhileController.Builder builder = new WhileController.Builder();
        builder.name(name);
        builder.whileSettings(whileSettings);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onWhile(String name,
                                            @DelegatesTo(strategy = DELEGATE_ONLY, value = WhileController.WhileSettings.Builder.class) Closure<?> cl,
                                            Worker steps) {
        WhileController.Builder builder = new WhileController.Builder();
        builder.name(name);
        builder.whileSettings(cl);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onWhileWith(String name,
                                                Customizer<WhileController.Builder> it,
                                                Worker steps) {
        WhileController.Builder builder = new WhileController.Builder();
        it.customize(builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onWhileWith(String name,
                                                @DelegatesTo(strategy = DELEGATE_ONLY, value = WhileController.Builder.class) Closure<?> cl,
                                                Worker steps) {
        WhileController.Builder builder = new WhileController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onWhileWith(String name,
                                                Customizer<WhileController.Builder> it) {
        WhileController.Builder builder = new WhileController.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onWhileWith(String name,
                                                @DelegatesTo(strategy = DELEGATE_ONLY, value = WhileController.Builder.class) Closure<?> cl) {
        WhileController.Builder builder = new WhileController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // GroupController

    public static DefaultTestResult group(String name,
                                          Worker steps) {
        GroupController.Builder builder = new GroupController.Builder();
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }


    public static DefaultTestResult groupWith(String name,
                                              Customizer<GroupController.Builder> it,
                                              Worker steps) {
        GroupController.Builder builder = new GroupController.Builder();
        it.customize(builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult groupWith(String name,
                                              @DelegatesTo(strategy = DELEGATE_ONLY, value = GroupController.Builder.class) Closure<?> cl,
                                              Worker steps) {
        GroupController.Builder builder = new GroupController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult groupWith(String name,
                                              Customizer<GroupController.Builder> it) {
        GroupController.Builder builder = new GroupController.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult groupWith(String name,
                                              @DelegatesTo(strategy = DELEGATE_ONLY, value = GroupController.Builder.class) Closure<?> cl) {
        GroupController.Builder builder = new GroupController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // IfController

    public static DefaultTestResult onIf(String name, String condition, Worker steps) {
        IfController.Builder builder = new IfController.Builder();
        builder.name(name);
        builder.condition(condition);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onIfWith(String name,
                                             Customizer<IfController.Builder> it,
                                             Worker steps) {
        IfController.Builder builder = new IfController.Builder();
        it.customize(builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onIfWith(String name,
                                             @DelegatesTo(strategy = DELEGATE_ONLY, value = IfController.Builder.class) Closure<?> cl,
                                             Worker steps) {
        IfController.Builder builder = new IfController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        builder.steps(steps);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onIfWith(String name,
                                             Customizer<IfController.Builder> it) {
        IfController.Builder builder = new IfController.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static DefaultTestResult onIfWith(String name,
                                             @DelegatesTo(strategy = DELEGATE_ONLY, value = IfController.Builder.class) Closure<?> cl) {
        IfController.Builder builder = new IfController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // TestCaseIncludeController

    public static RefTestCaseResult refTestCase(String name, Object testcase, Map<String, ?> variables) {
        RefTestCaseController.Builder builder = new RefTestCaseController.Builder();
        builder.name(name);
        builder.refTestCase(testcase);
        builder.config(config -> config
            .variables(vars -> {
                if (variables != null) {
                    variables.forEach(vars::var);
                }
            }));
        return getSession().run(builder.build());
    }

    public static RefTestCaseResult refTestCaseWith(String name,
                                                    Customizer<RefTestCaseController.Builder> it) {
        RefTestCaseController.Builder builder = new RefTestCaseController.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static RefTestCaseResult refTestCaseWith(String name,
                                                    @DelegatesTo(strategy = DELEGATE_ONLY, value = RefTestCaseController.Builder.class) Closure<?> cl) {
        RefTestCaseController.Builder builder = new RefTestCaseController.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // NoopSampler

    public static DefaultSampleResult noopWith(String name,
                                           Customizer<NoopSampler.Builder> it) {
        NoopSampler.Builder builder = new NoopSampler.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static DefaultSampleResult noopWith(String name,
                                              @DelegatesTo(strategy = DELEGATE_ONLY, value = NoopSampler.Builder.class) Closure<?> cl) {
        NoopSampler.Builder builder = new NoopSampler.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

}
