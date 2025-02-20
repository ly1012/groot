package com.liyunx.groot.testelement;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.context.Context;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.SessionContext;
import com.liyunx.groot.testelement.controller.AbstractContainerController;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用例类，最上层的测试元件。
 * <p>
 * 驱动数据的管理和执行由用户决定，Groot 只提供单个具体 Test Case 的运行。
 * 比如与 TestNG 集成时，可以使用 @DataProvider 或监听器配合注解等方式实现，拿到一组驱动数据后放入 SessionRunner 对象。与平台集成时类似。
 *
 * <p>用例调用者的上下文数据将覆盖被调用用例的上下文数据（具体实现时并不会真的创建被调用用例上下文，而是直接合并），具体来说：
 * <ul>
 *     <li>直接执行测试用例时，最终的 SessionContext 为 SessionContext 合并被执行用例的 TestStepContext，
 *     合并时 SessionContext 优先级更高</li>
 *     <li>执行引用用例时，使用新的 SessionRunner，最终的 SessionContext 为调用者 TestStepContext 合并被执行用例的 TestStepContext，
 *     合并时调用者的 TestStepContext 优先级更高，即调用者 TestStepContext 合并进 SessionContext，
 *     然后执行被引用用例时 SessionContext 合并被执行用例的 TestStepContext</li>
 * </ul>
 *
 * <p>用例参数化数据（驱动数据）和步骤参数化数据的区别在于：
 * 用例参数化会形成多个测试用例，每个用例之间相互独立，一个用例失败，不影响另一个用例的运行，在测试报告上体现为运行了多个用例；
 * 步骤参数化是在一个用例内进行的，循环时第一组参数执行失败，则用例失败，不会继续执行剩余参数数据。
 * <ul>
 *     <li>用例参数化举例：使用不同的用户名和密码组合，对用户登录接口进行接口测试，
 *     参数列比如：用户名、密码等请求信息，状态码、响应 JSON等断言信息</li>
 *     <li>步骤参数化举例：GitHub 新增多个 Issue，然后批量关闭这些新增的 Issue，可以将要新增的多个 Issue 数据放入参数化表格</li>
 * </ul>
 */
@KeyWord(TestCase.KEY)
public class TestCase extends AbstractContainerController<TestCase, DefaultTestResult> {

    public static final String KEY = "__testcase__";

    // TODO Yaml/Json 用例配置文件是直接加载为 TestCase 或加载为 MultiTestCase 后转为 TestCase，暂时未定。
    //protected List<Map<String, Object>> parameters;


    public TestCase() {
    }

    private TestCase(Builder builder) {
        super(builder);
    }

    @Override
    public void execute(ContextWrapper ctx, DefaultTestResult result) {
        executeSubSteps(ctx);
    }

    // ----------- 方法重写 -------------

    @Override
    protected DefaultTestResult createTestResult() {
        return new DefaultTestResult();
    }

    // 重写该方法，因为 SessionContext 在 SessionRunner 中创建，需要特殊处理，
    // 这里更新 SessionContext，而非新建。
    @Override
    protected List<Context> getContextChain(List<Context> parentContext) {
        List<Context> contextChain = new ArrayList<>(parentContext);
        updateCurrentContext(contextChain);
        return contextChain;
    }

    private void updateCurrentContext(List<Context> contextChain) {
        Context context = contextChain.get(contextChain.size() - 1);
        if (context instanceof SessionContext sessionContext) {
            if (this.running.config == null)
                this.running.config = new TestElementConfig();
            // 驱动数据覆盖默认变量值
            TestElementConfig configGroup = (TestElementConfig) this.running.config.merge(sessionContext.getConfigGroup());
            this.running.config = configGroup;
            sessionContext.setConfigGroup(configGroup);
            return;
        }
        throw new IllegalStateException("上下文链非法，缺失 SessionContext");
    }

    // ---------------------------------------------------------------------
    // Builder (TestCase.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractContainerController.Builder<TestCase, Builder>
        implements TestElementBuilder<TestCase> {

        @Override
        public TestCase build() {
            return new TestCase(this);
        }

    }

}
