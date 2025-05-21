package com.liyunx.groot.testelement.controller;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.Context;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.SessionContext;
import com.liyunx.groot.context.TestStepContext;
import com.liyunx.groot.dataloader.DataLoader;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.testelement.TestCase;
import com.liyunx.groot.testelement.TestElementBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试步骤：引用测试用例
 * <p>
 * 执行引用用例时，会忽视可能存在的用例驱动数据（用例参数化数据），本步骤 config.variables 中给定的变量将代替原本由驱动数据设置的变量。
 */
@KeyWord(RefTestCaseController.KEY)
public class RefTestCaseController extends AbstractIncludeController<RefTestCaseController, RefTestCaseResult> {

    public static final String KEY = "testcase";

    /* ------------------------------------------------------------ */
    // 声明时数据，不可修改

    @JSONField(name = RefTestCaseController.KEY)
    private Object testcase;

    public RefTestCaseController() {
    }

    private RefTestCaseController(Builder builder) {
        super(builder);
        this.testcase = builder.testcase;
    }

    @Override
    protected RefTestCaseResult createTestResult() {
        return new RefTestCaseResult();
    }

    @Override
    protected TestStepContext createCurrentContext() {
        // TestCase 引用涉及到跨级访问，需要特殊处理，这里不采用测试步骤的反转覆盖方案
        TestStepContext context = super.createCurrentContext();
        context.setInvert(0);
        return context;
    }

    @Override
    protected void execute(ContextWrapper ctx, RefTestCaseResult result) {
        TestCase refTestCase = loadTestCase(testcase, ctx);

        // 执行其他用例时需要考虑的 3 个影响因素：
        // storage：使用新 SessionRunner 隔离
        // 上下文的变量配置，变量读取、变量修改、变量增加或减少
        // 上下文的其他配置，比如 Http 配置中的 baseUrl

        // 执行被引用测试用例，两种情况：
        // (1) 使用当前 SessionRunner，
        // 当前父上下文链为 [... SessionContext ParentStepContext CurrentStepContext RefCaseContext]。
        // 暂时不做，什么场景下需要使用当前的 SessionRunner，好像不需要？遇到了再总结实现，不提前实现不需要的功能。
        // (2) 使用新的 SessionRunner，用例调用者的上下文数据覆盖被调用用例的上下文数据。
        // 继承当前 SessionRunner 的可继承数据（通过调用 inheritSessionRunner 方法），
        // 使用新的 SessionContext 对象，然后执行引用用例，即父上下文链到新 SessionContext 为止。

        SessionRunner session = ctx.getSessionRunner().inheritSessionRunner();
        session.getStorage().put(TEST_STEP_NUMBER_STACK, ctx.getSessionRunner().getStorage().get("__TestStep_Number_Stack__"));
        session.getStorage().put(TEST_STEP_NUMBER_PREVIOUS_NO, 0);

        // 上下文链处理
        // 默认处理策略：保留上下文链中的所有上下文
        // 可能的其他策略（暂不支持）：
        // （1）仅保留 testRunner 及之前的上下文
        // session.config(running.config);
        // （2）仅保留 sessionRunner 及之前的上下文

        // ---配置覆盖（包括变量）---
        // 保留本步骤及之前的所有配置数据，执行被引用用例时，使用这些配置。
        // 比如已声明的 HTTP 配置（baseUrl、Proxy 等）、Extract 提取器默认提取作用域配置等等。
        //
        // ---处理逻辑---
        // 越靠前优先级越低，即后面的会覆盖前面的，默认处理策略的上下文链路：
        // globalContext > environmentContext > testRunnerContext > 新 sessionContext
        // 其中，
        // 新 sessionContext 之前的上下文和当前 SessionRunner 相同
        // 新 sessionContext 的处理逻辑为：
        // 1. 获取当前 SessionRunner 上下链中从 SessionContext 开始到本步骤为止的所有上下文，合并后作为新 session 的 SessionContext
        // 2. 新 session 执行被引用用例时，新 SessionContext 覆盖被引用用例 TestCase 上声明的上下文配置
        List<Context> contextChain = new ArrayList<>();
        boolean start = false;
        for (Context context : ctx.getSessionRunner().getContextChain()) {
            if (start) {
                contextChain.add(context);
            } else if (context instanceof SessionContext) {
                start = true;
                contextChain.add(context);
            }
        }
        TestElementConfig mergedConfigGroup = (TestElementConfig) new ContextWrapper(contextChain).getConfigGroup();
        session.config(mergedConfigGroup);
        SessionRunner previousSession = SessionRunner.getSession();
        SessionRunner.setSession(session);
        session.run(refTestCase);
        SessionRunner.setSession(previousSession);

        // 变量同步：提取、更新
        VariableConfigItem refVars = refTestCase.getRunning().getConfig().getVariableConfigItem();
        VariableConfigItem vars = running.config.getVariableConfigItem();
        Map<String, Object> variables = result.getVariables();
        // 以声明时变量列表为基准
        config.getVariableConfigItem().forEach((k, v) -> {
            Object value = refVars.get(k);
            vars.put(k, value);
            variables.put(k, value);
        });
    }

    private TestCase loadTestCase(Object testcase, ContextWrapper ctx) {
        if (testcase instanceof String) {
            String testCaseId = ctx.evalAsString((String) testcase);
            // load TestCase Data
            DataLoader dataLoader = ctx.getSessionRunner().getConfiguration().getDataLoader();
            return dataLoader.loadByID(testCaseId, TestCase.class);
        }
        if (testcase instanceof TestCase) {
            return ((TestCase) testcase).copy();
        }
        throw new GrootException("caseId 必须是 String 或 " + TestCase.class.getName() + " 类型");
    }

    @Override
    public RefTestCaseController copy() {
        RefTestCaseController self = super.copy();
        self.testcase = testcase;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (TestCaseIncludeController)
    // ---------------------------------------------------------------------

    public Object getTestcase() {
        return testcase;
    }

    public void setTestcase(Object testcase) {
        this.testcase = testcase;
    }

    // ---------------------------------------------------------------------
    // Builder (TestCaseIncludeController.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractIncludeController.Builder<RefTestCaseController, Builder, RefTestCaseResult>
        implements TestElementBuilder<RefTestCaseController> {

        private Object testcase;

        /**
         * 执行其他测试用例
         *
         * @param testCaseId 其他用例标识符
         * @return 当前对象
         */
        public Builder refTestCase(String testCaseId) {
            this.testcase = testCaseId;
            return this;
        }

        /**
         * 执行其他测试用例
         *
         * @param testCase 其他用例对象
         * @return 当前对象
         */
        public Builder refTestCase(TestCase testCase) {
            this.testcase = testCase;
            return this;
        }

        /**
         * 执行其他测试用例
         *
         * @param caseIdOrTestCase 其他用例标识符或对象
         * @return 当前对象
         */
        public Builder refTestCase(Object caseIdOrTestCase) {
            if (!(caseIdOrTestCase instanceof String || caseIdOrTestCase instanceof TestCase)) {
                throw new IllegalArgumentException("caseIdOrTestCase 必须是 String 或 " + TestCase.class.getName() + " 类型");
            }
            this.testcase = caseIdOrTestCase;
            return this;
        }

        @Override
        public RefTestCaseController build() {
            return new RefTestCaseController(this);
        }

    }

}
