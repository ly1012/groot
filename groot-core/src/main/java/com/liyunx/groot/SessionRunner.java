package com.liyunx.groot;

import com.liyunx.groot.builder.AllConfigBuilder;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.Context;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.SessionContext;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.testelement.DefaultTestResult;
import com.liyunx.groot.testelement.TestCase;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.TestResult;
import com.liyunx.groot.testelement.controller.RefTestCaseController;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用例实际执行入口，非线程安全类，每个用例应使用各自的 SessionRunner 对象。
 *
 * <p>
 * 第一次运行速度较慢，需要初始化的模块（懒加载）(相对时间）：
 * <pre>
 * - HttpClient: 比如 OkHttpClient    1100 ms
 * - logback                          800  ms
 * - JSON                             400  ms
 * - FreeMarker                       220  ms
 * - Yaml                             120  ms
 * - SPI                              30   ms
 * </pre>
 * 如果是平台运行，会提前预热或第一次执行时预热，所以用例执行会很快（毫秒级）。
 * 但如果是 IDE 执行，每次执行都会重新初始化（代码变更需要重新编译执行），速度会比较慢（可能在 1 ~ 3 秒）。
 * 即使是 JVM 上的脚本语言，如 Groovy，每次执行仍然会需要初始化，这点不会因脚本语言而加快，因为初始化是在类库的代码逻辑！
 * 解释性语言，如 Python、JS 的第一次运行会非常快（毫秒级，无需提前编译，类库一般也没有初始化预热），
 * 但大量用例运行速度较 Java、Go 等语言慢。
 * <p>
 * 本机不严谨测试，运行 10 万次 Http Get 请求（基于 WireMock，连接复用），requests(Python) 较 Groot(Java) 慢 6 倍左右。
 * 总的来说，Python、JS 等解释性语言第一次运行（本地调试）有明显优势，但大量用例的执行速度（CI）有劣势。
 * 考虑自动化测试用例实际应用：
 * - 每次用例开发或修改完，需要运行一次，因此基于 IDE 的开发效率上解释性语言可能更占优势。
 * - 用例耗时主要来自 SUT 的接口 RT、SQL Assert 的 RT 等与 SUT 强相关的地方，因此用例执行时语言执行效率倒是次要因素。
 * - 用例开发时间与用例运行时间相比（接口类，非 UI 类），用例运行时间比例很小，因此第一次运行时长的影响并不是很大。
 * - 结合上面几点，实际自动化测试项目中，更应该关注用例编写效率、排查效率、维护成本、交接成本、工具语言的测试生态、
 * 被测系统语言、公司基建等因素，工具的执行效率（第一次运行效率、语言性能）并非主要影响因素。
 */
public class SessionRunner {

    private final TestRunner testRunner;
    private final SessionContext sessionContext = new SessionContext();

    /**
     * Session 当前执行上下文链
     */
    private List<Context> contextChain = new ArrayList<>();
    private ContextWrapper contextWrapper;

    /**
     * 用例执行过程中可用来进行数据存取
     */
    private final Map<String, Object> storage = new HashMap<>();

    public static final ThreadLocal<SessionRunner> HOLDER = new ThreadLocal<>();

    public static SessionRunner getSession() {
        SessionRunner sessionRunner = HOLDER.get();
        if (sessionRunner == null) {
            throw new IllegalStateException("SessionRunner 未设置，请先调用 setSession 方法，或使用 TestNG 组件的 @GrootSupport 等特性");
        }
        return sessionRunner;
    }

    public static void setSession(SessionRunner sessionRunner) {
        HOLDER.set(sessionRunner);
    }

    public static void removeSession() {
        HOLDER.remove();
    }

    SessionRunner(TestRunner testRunner) {
        this.testRunner = testRunner;
        initContextChain();
    }

    private void initContextChain() {
        Groot groot = testRunner.getGroot();

        // 全局上下文
        contextChain.add(groot.getGlobalContext());
        // 环境上下文
        contextChain.add(groot.getEnvironmentContext());
        // 用例上下文（用例参数化，对应一个或多个运行用例）
        contextChain.add(testRunner.getTestContext());
        // 会话上下文（运行用例）
        contextChain.add(sessionContext);

        // 会话上下文默认值：添加一个空的变量配置
        // SessionRunner 可能直接运行某个 Sampler，而不是 TestCase，比如 Groovy/Java 用例
        // 示例：SessionRunner 连续运行多个 Http 请求，Http 请求中设置和读取 Session 变量
        TestElementConfig testElementConfig = new TestElementConfig();
        testElementConfig.put(VariableConfigItem.KEY, new VariableConfigItem());
        sessionContext.setConfigGroup(testElementConfig);

        contextWrapper = new ContextWrapper(this);
    }

    /**
     * 创建一个新的 SessionRunner，并继承当前 SessionRunner 的可共享数据。
     *
     * <p>比如每个用例会创建一个 WebDriver 对象，执行引用用例时，共享该 WebDriver 对象，而不是再创建一个新的 WebDriver 对象。
     *
     * @return 包含共享数据的新 SessionRunner
     */
    public SessionRunner inheritSessionRunner() {
        SessionRunner newSession = new SessionRunner(testRunner);
        // 共享数据继承
        ApplicationConfig.getSessionRunnerInheritances().forEach(inheritance -> {
            inheritance.inheritSessionRunner(this, newSession);
        });
        return newSession;
    }

    /**
     * Session 上下文配置，如变量配置、Http 配置等。
     */
    public void config(TestElementConfig config) {
        TestElementConfig oldConfig = (TestElementConfig) sessionContext.getConfigGroup();
        sessionContext.setConfigGroup(oldConfig.merge(config));
    }

    /**
     * @see #config(TestElementConfig)
     */
    public void config(Customizer<AllConfigBuilder> config) {
        AllConfigBuilder testElementConfigBuilder = new AllConfigBuilder();
        config.customize(testElementConfigBuilder);
        config(testElementConfigBuilder.build());
    }

    /**
     * @see #config(TestElementConfig)
     */
    public void config(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = AllConfigBuilder.class) Closure<?> cl) {
        AllConfigBuilder testElementConfigBuilder = new AllConfigBuilder();
        GroovySupport.call(cl, testElementConfigBuilder);
        config(testElementConfigBuilder.build());
    }

    /**
     * 运行任意的测试元件，如 TestCase 或 Sampler
     * <p>
     * 默认校验：执行前校验测试元件数据是否合法
     *
     * @see #run(TestElement, boolean)
     */
    public <T extends TestResult<T>> T run(TestElement<T> testElement) {
        return run(testElement, true);
    }

    /**
     * 运行任意的测试元件，如 TestCase 或 Sampler
     * <p>
     * 不支持运行多个 TestCase。直接运行 TestCase 对象时，每个 TestCase 应使用各自的 SessionRunner 对象。
     * 否则会破坏测试报告数据，另外也不合乎常理，如果支持就变成了一个用例执行，实际是在执行多个用例和交叉的步骤。
     * 一个用例中要执行其他用例，请使用 {@link RefTestCaseController}。
     *
     * @param testElement 测试元件
     * @param <T>         测试元件对应的执行结果类
     * @param validate    是否校验测试元件数据
     * @return 测试元件的执行结果
     */
    public <T extends TestResult<T>> T run(TestElement<T> testElement, boolean validate) {
        if (validate) {
            ValidateResult validateResult = testElement.validate();
            if (!validateResult.isValid()) {
                throw new InvalidDataException(validateResult.getReason());
            }
        }
        return testElement.run(this);
    }

    public DefaultTestResult run(String testCaseIdentifier) {
        TestCase testCase = getConfiguration().getDataLoader().loadByID(testCaseIdentifier, TestCase.class);
        return run(testCase);
    }

    /**
     * 执行初始化动作
     */
    public void start() {
        ApplicationConfig.getSessionRunnerListeners().forEach(listener -> listener.sessionRunnerStart(this));
    }

    /**
     * 执行清理动作
     */
    public void stop() {
        ApplicationConfig.getSessionRunnerListeners().forEach(listener -> listener.sessionRunnerStop(this));
    }

    /**
     * 获取 TestCaseRunner 当前的上下文链，依次为：全局上下文、环境上下文、用例上下文。
     *
     * @return TestCaseRunner 当前的上下文链
     */
    public List<Context> getContextChain() {
        return contextChain;
    }

    public void setContextChain(List<Context> contextChain) {
        this.contextChain = contextChain;
    }

    public ContextWrapper getContextWrapper() {
        return contextWrapper;
    }

    public void setContextWrapper(ContextWrapper contextWrapper) {
        this.contextWrapper = contextWrapper;
    }

    public TestRunner getTestRunner() {
        return testRunner;
    }

    public Configuration getConfiguration() {
        return testRunner.getGroot().getConfiguration();
    }

    public Map<String, Object> getStorage() {
        return storage;
    }

}
