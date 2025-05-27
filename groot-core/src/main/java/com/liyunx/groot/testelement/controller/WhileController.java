package com.liyunx.groot.testelement.controller;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.builder.TestBuilder;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.testelement.TestElementBuilder;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.template.TemplateEngine;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.testelement.DefaultTestResult;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import static groovy.lang.Closure.DELEGATE_ONLY;

/**
 * 循环控制器，循环执行子测试元件直到条件不满足
 */
@KeyWord(WhileController.KEY)
public class WhileController extends AbstractContainerController<WhileController, DefaultTestResult> {

    public static final String KEY = "while";

    /* ------------------------------------------------------------ */
    // 声明时数据，不可修改

    @JSONField(name = KEY)
    private WhileSettings whileSettings;

    /* ------------------------------------------------------------ */
    // 运行时数据，对外开放

    private int loopCount;

    public WhileController() {
    }

    private WhileController(Builder builder) {
        super(builder);
        this.whileSettings = builder.whileSettings;
    }

    @Override
    protected DefaultTestResult createTestResult() {
        return new DefaultTestResult();
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();
        if (whileSettings == null) {
            r.append("\n字段：WhileController.while，while 设置缺失");
        } else {
            String condition = whileSettings.getCondition();
            if (condition == null
                || (!TemplateEngine.hasExpression(condition)
                && !condition.trim().equalsIgnoreCase("true")
                && !condition.trim().equalsIgnoreCase("false"))) {
                r.append("\n字段：WhileController.while.condition，条件表达式 %s 不是合法的布尔值字符串（true/false）或插值表达式", condition);
            }
        }
        return r;
    }

    @Override
    protected void execute(ContextWrapper ctx, DefaultTestResult result) {
        long startTime = System.currentTimeMillis();  //开始执行时间
        long endTime = 0;
        long timeout = whileSettings.getTimeout();    //超时设定（单位：ms）
        int cnt = 0;                                  //已执行次数
        int limit = whileSettings.getLimit();         //超次设定

        loopCount = 0;
        while (evalCondition(ctx, whileSettings.getCondition())) {
            if (cnt >= limit || (endTime - startTime) > timeout) {
                return;
            }
            loopCount = cnt + 1;
            withTestStepNumberLog(ctx, loopCount, () -> executeSubSteps(ctx));
            cnt++;
            endTime = System.currentTimeMillis();
        }
    }

    private boolean evalCondition(ContextWrapper ctx, String condition) {
        return Boolean.parseBoolean(ctx.evalAsString(condition));
    }

    @Override
    public WhileController copy() {
        WhileController self = super.copy();
        self.whileSettings = whileSettings;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (WhileController)
    // ---------------------------------------------------------------------

    public WhileSettings getWhileSettings() {
        return whileSettings;
    }

    public void setWhileSettings(WhileSettings whileSettings) {
        this.whileSettings = whileSettings;
    }

    public long getLoopCount() {
        return loopCount;
    }

    // ---------------------------------------------------------------------
    // Builder (WhileController.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractContainerController.Builder<WhileController, Builder, DefaultTestResult>
        implements TestElementBuilder<WhileController> {

        private WhileSettings whileSettings;

        /**
         * While 控制器设置
         *
         * @param whileSettings While 控制器设置函数
         * @return 当前对象
         */
        public Builder whileSettings(Customizer<WhileSettings.Builder> whileSettings) {
            WhileSettings.Builder builder = new WhileSettings.Builder();
            whileSettings.customize(builder);
            this.whileSettings = builder.build();
            return this;
        }

        /**
         * While 控制器设置
         *
         * @param whileSettings While 控制器设置闭包
         * @return 当前对象
         */
        public Builder whileSettings(@DelegatesTo(strategy = DELEGATE_ONLY, value = WhileSettings.Builder.class) Closure<?> whileSettings) {
            WhileSettings.Builder builder = new WhileSettings.Builder();
            GroovySupport.call(whileSettings, builder);
            this.whileSettings = builder.build();
            return this;
        }

        /**
         * While 控制器设置
         *
         * @param whileSettings While 控制器设置
         * @return 当前对象
         */
        public Builder whileSettings(WhileSettings whileSettings) {
            this.whileSettings = whileSettings;
            return this;
        }

        @Override
        public WhileController build() {
            return new WhileController(this);
        }

    }

    /**
     * While 循环设置
     */
    public static class WhileSettings {

        public static final String CONDITION_FIELD = "condition";

        /**
         * 条件表达式，表达式返回 true 则继续循环，否则结束循环
         */
        @JSONField(name = CONDITION_FIELD)
        private String condition;

        /**
         * 超时设定，超时则结束循环，单位：ms
         */
        @JSONField(name = "timeout")
        private long timeout = Long.MAX_VALUE;

        /**
         * 超次设定，超过指定循环次数则结束循环
         */
        @JSONField(name = "limit")
        private int limit = Integer.MAX_VALUE;

        // TODO 变量别名，可以自定义变量保存当前循环次数的值

        public WhileSettings() {
        }

        private WhileSettings(Builder builder) {
            this.condition = builder.condition;
            this.timeout = builder.timeout;
            this.limit = builder.limit;
        }

        // ---------------------------------------------------------------------
        // Getter/Setter (WhileController.WhileSettings)
        // ---------------------------------------------------------------------

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        // ---------------------------------------------------------------------
        // Builder (WhileController.WhileSettings.Builder)
        // ---------------------------------------------------------------------

        public static class Builder implements TestBuilder<WhileSettings> {

            private String condition;
            private long timeout = Long.MAX_VALUE;
            private int limit = Integer.MAX_VALUE;

            /**
             * 条件表达式
             *
             * @param condition 表达式返回 true 则继续循环，否则结束循环
             * @return 当前对象
             */
            public Builder condition(String condition) {
                this.condition = condition;
                return this;
            }

            /**
             * 超时设定，超时则结束循环，单位：ms
             *
             * @param timeoutInMillis 超时时间
             * @return 当前对象
             */
            public Builder timeout(long timeoutInMillis) {
                this.timeout = timeoutInMillis;
                return this;
            }

            /**
             * 超次设定，超过指定循环次数则结束循环
             *
             * @param limit 超次次数
             * @return 当前对象
             */
            public Builder limit(int limit) {
                this.limit = limit;
                return this;
            }

            @Override
            public WhileSettings build() {
                return new WhileSettings(this);
            }
        }

    }

}
