package com.liyunx.groot.context;

import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.config.ConfigGroup;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.context.variables.*;
import com.liyunx.groot.template.TemplateEngine;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.TestResult;
import com.liyunx.groot.util.CollectionUtil;

import java.util.*;

/**
 * 测试上下文包装器，提供了处理上下文链的各种方法。
 */
public class ContextWrapper {

    // 原始的上下文作用域链
    private final List<Context> rawContextChain;
    // 反转后的上下文作用域链
    private final List<Context> invertedContextChain;

    // 特殊上下文
    private GlobalContext globalContext;
    private EnvironmentContext environmentContext;
    private TestContext testContext;
    private SessionContext sessionContext;
    // 当前上下文，即最后一个上下文对象
    private final Context currentContext;

    // 变量包装器
    private GlobalVariablesWrapper globalVariablesWrapper;
    private EnvironmentVariablesWrapper environmentVariablesWrapper;
    private TestVariablesWrapper testVariablesWrapper;
    private SessionVariablesWrapper sessionVariablesWrapper;
    private final AllVariablesWrapper allVariablesWrapper;
    private final LocalVariablesWrapper localVariablesWrapper;

    // 合并后的配置组
    private final ConfigGroup configGroup;

    // Runner 相关对象
    private TemplateEngine templateEngine;
    private SessionRunner sessionRunner;

    // 测试元件信息
    private TestElement<?> testElement;
    private TestResult<?> testResult;

    // ----------- 构造器 -------------

    /**
     * 用于用例执行前，比如全局变量和环境变量的表达式计算
     *
     * @param contextChain 上下文链
     */
    public ContextWrapper(List<Context> contextChain) {
        requiredNotNullAndNotEmpty(contextChain);

        // 处理上下文链
        this.rawContextChain = contextChain;
        this.invertedContextChain = getInvertContextChain();

        // 合并配置组
        // TODO 懒加载？
        this.configGroup = mergeConfigGroup();

        // 读取各级上下文
        searchNonTestStepContext();
        this.currentContext = contextChain.get(contextChain.size() - 1);

        // 处理各级上下文
        // TODO session 以及更上层的变量包装器，实例化一次就够了?
        if (globalContext != null) {
            this.globalVariablesWrapper = new GlobalVariablesWrapper(CollectionUtil.listOf(globalContext));
        }
        if (environmentContext != null) {
            this.environmentVariablesWrapper = new EnvironmentVariablesWrapper(CollectionUtil.listOf(environmentContext));
        }
        if (testContext != null) {
            this.testVariablesWrapper = new TestVariablesWrapper(CollectionUtil.listOf(testContext));
        }
        if (sessionContext != null) {
            this.sessionVariablesWrapper = new SessionVariablesWrapper(CollectionUtil.listOf(sessionContext));
        }
        this.allVariablesWrapper = new AllVariablesWrapper(Collections.unmodifiableList(invertedContextChain));
        this.localVariablesWrapper = new LocalVariablesWrapper(CollectionUtil.listOf(currentContext));
    }

    /**
     * 用于用例执行时，包装当前元件的执行信息
     *
     * @param sessionRunner 用例执行器
     */
    public ContextWrapper(SessionRunner sessionRunner) {
        this(sessionRunner.getContextChain());
        this.sessionRunner = sessionRunner;
        this.templateEngine = sessionRunner.getConfiguration().getTemplateEngine();
    }

    // == 构造器辅助方法 ==

    private void requiredNotNullAndNotEmpty(List<Context> contextChain) {
        if (contextChain == null || contextChain.isEmpty()) {
            throw new UnsupportedOperationException("上下文链为空");
        }

        long nullCount = contextChain.stream().filter(Objects::isNull).count();
        if (nullCount > 0) {
            throw new UnsupportedOperationException("上下文链中存在空值");
        }
    }

    private List<Context> getInvertContextChain() {
        List<Context> invertContextChain = new ArrayList<>(rawContextChain);
        for (int i = 0; i < invertContextChain.size(); i++) {
            Context context = invertContextChain.get(i);
            int revert = context.getInvert();
            if (revert > 1) {
                throw new UnsupportedOperationException("上下文链反转失败，当前仅支持一级反转，实际反转层级：" + revert);
            }

            if (revert > 0 && i + revert < invertContextChain.size()) {
                Collections.swap(invertContextChain, i, i + revert);
                i++;
            }
        }
        return invertContextChain;
    }

    private void searchNonTestStepContext() {
        // 如果上下文链中存在多个 SessionContext，则取最后一个，即最近的一个
        rawContextChain.forEach(ctx -> {
            if (ctx instanceof GlobalContext) {
                this.globalContext = (GlobalContext) ctx;
            } else if (ctx instanceof EnvironmentContext) {
                this.environmentContext = (EnvironmentContext) ctx;
            } else if (ctx instanceof TestContext) {
                this.testContext = (TestContext) ctx;
            } else if (ctx instanceof SessionContext) {
                this.sessionContext = (SessionContext) ctx;
            }
        });
    }

    private ConfigGroup mergeConfigGroup() {
        ConfigGroup config = new TestElementConfig();
        for (Context context : invertedContextChain) {
            ConfigGroup configGroup = context.getConfigGroup();
            config = config.merge(configGroup);
        }
        return config;
    }

    // ----------- ContextWrapper 对外 API -------------

    /**
     * @see TemplateEngine#eval(ContextWrapper, Object)
     */
    public Object eval(Object obj) {
        return templateEngine.eval(this, obj);
    }

    public Object eval(Object obj, boolean force) {
        return templateEngine.eval(this, obj, force);
    }

    /**
     * @see TemplateEngine#eval(ContextWrapper, Map)
     */
    public Map<String, String> eval(Map<String, String> map) {
        return templateEngine.eval(this, map);
    }

    /**
     * @see TemplateEngine#eval(ContextWrapper, List)
     */
    public List<String> eval(List<String> list) {
        return templateEngine.eval(this, list);
    }

    /**
     * 模板计算
     *
     * @param text 模板字符串
     * @return 模板计算结果
     */
    public Object eval(String text) {
        return templateEngine.eval(this, text);
    }

    /**
     * 当且仅当参数对象的类型为 String 类型时，才进行计算，否则直接返回。
     *
     * @param obj 待计算对象
     * @return 计算结果（仅当参数为 String 类型时才会计算）
     */
    public Object evalIfString(Object obj) {
        if (obj instanceof String) {
            return templateEngine.eval(this, obj);
        }
        return obj;
    }

    /**
     * 模板计算（不会返回 null，但当参数为 null 时返回 null 字符串）
     *
     * @param text 模板字符串
     * @return 模板计算结果的字符串表示，如果参数为 null，则返回 null 字符串
     */
    public String evalAsString(String text) {
        return String.valueOf(eval(text));
    }

    // == Getter/Setter ==

    public List<Context> getContextChain() {
        return rawContextChain;
    }

    public GlobalVariablesWrapper getGlobalVariablesWrapper() {
        return globalVariablesWrapper;
    }

    public EnvironmentVariablesWrapper getEnvironmentVariablesWrapper() {
        return environmentVariablesWrapper;
    }

    public TestVariablesWrapper getTestVariablesWrapper() {
        return testVariablesWrapper;
    }

    public SessionVariablesWrapper getSessionVariablesWrapper() {
        return sessionVariablesWrapper;
    }

    public AllVariablesWrapper getAllVariablesWrapper() {
        return allVariablesWrapper;
    }

    public LocalVariablesWrapper getLocalVariablesWrapper() {
        return localVariablesWrapper;
    }

    public SessionRunner getSessionRunner() {
        return sessionRunner;
    }

    public ConfigGroup getConfigGroup() {
        return configGroup;
    }

    public TestElement<?> getTestElement() {
        return testElement;
    }

    public void setTestElement(TestElement<?> testElement) {
        this.testElement = testElement;
    }

    public TestResult<?> getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResult<?> testResult) {
        this.testResult = testResult;
    }
}
