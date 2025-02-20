package com.liyunx.groot.testelement.controller;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.testelement.TestElementBuilder;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.template.TemplateEngine;
import com.liyunx.groot.testelement.DefaultTestResult;

/**
 * 条件控制器
 */
@KeyWord(IfController.KEY)
public class IfController extends AbstractContainerController<IfController, DefaultTestResult> {

    public static final String KEY = "if";

    /* ------------------------------------------------------------ */
    // 声明时数据，不可修改

    /**
     * 条件布尔值或条件表达式
     * <p>不支持运行时修改（TestFilter）
     */
    @JSONField(name = IfController.KEY)
    private String condition;

    /* ------------------------------------------------------------ */
    // 运行时数据，对外开放

    private boolean satisfied = false;

    public IfController() {
    }

    private IfController(Builder builder) {
        super(builder);
        this.condition = builder.condition;
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();
        if (condition == null
            || (!TemplateEngine.hasExpression(condition)
            && !condition.trim().equalsIgnoreCase("true")
            && !condition.trim().equalsIgnoreCase("false"))) {
            r.append(String.format("\n字段：IfController.if，条件表达式 %s 不是合法的布尔值字符串（true/false）或插值表达式", condition));
        }
        return r;
    }

    @Override
    public void execute(ContextWrapper ctx, DefaultTestResult result) {
        satisfied = false;
        String res = ctx.evalAsString(condition).trim();
        if ("true".equalsIgnoreCase(res) || "false".equalsIgnoreCase(res)) {
            if (Boolean.parseBoolean(res)) {
                satisfied = true;
                executeSubSteps(ctx);
            }
        } else {
            String err;
            if (TemplateEngine.hasExpression(condition)) {
                err = String.format("IfController.if 条件表达式 %s 的计算结果 %s 不是合法的布尔值字符串（true/false）或插值表达式", condition, res);
            } else {
                err = String.format("IfController.if 条件表达式 %s 的计算结果 %s 不是合法的布尔值字符串（true/false）或插值表达式", res, res);
            }
            throw new IllegalArgumentException(err);
        }
    }

    @Override
    protected DefaultTestResult createTestResult() {
        return new DefaultTestResult();
    }

    @Override
    public IfController copy() {
        IfController self = super.copy();
        self.condition = condition;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (IfController)
    // ---------------------------------------------------------------------

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean getSatisfied() {
        return satisfied;
    }

    // ---------------------------------------------------------------------
    // Builder (IfController.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractContainerController.Builder<IfController, Builder>
        implements TestElementBuilder<IfController> {

        private String condition;

        /**
         * IF 条件表达式
         *
         * @param condition 表达式，其结算结果必须是布尔类型
         * @return 当前对象
         */
        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        @Override
        public IfController build() {
            return new IfController(this);
        }

    }

}
