package com.liyunx.groot.testelement.controller;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.TestStepContext;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.template.TemplateEngine;
import com.liyunx.groot.testelement.DefaultTestResult;
import com.liyunx.groot.testelement.TestElementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 循环控制器，循环执行子测试元件指定次数
 */
@KeyWord(RepeatController.KEY)
public class RepeatController extends AbstractContainerController<RepeatController, DefaultTestResult> {

    private static final Logger log = LoggerFactory.getLogger(RepeatController.class);

    public static final String KEY = "repeat";
    public static final String CURRENT_LOOP_COUNT = "loopCount";

    /* ------------------------------------------------------------ */
    // 声明时数据，不可修改

    @JSONField(name = RepeatController.KEY)
    private String times;

    // TODO 变量别名，默认是 loopCount，可以自定义变量保存当前循环次数的值

    /* ------------------------------------------------------------ */
    // 运行时数据，对外开放

    private int loopCount;

    public RepeatController() {
    }

    private RepeatController(Builder builder) {
        super(builder);
        this.times = builder.loopsAsString;
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();
        if (times == null || times.trim().isEmpty()) {
            r.append("\n字段：LoopController.loop，循环次数为空，请指定循环次数");
        } else if (!TemplateEngine.hasExpression(times)) {
            try {
                Integer.parseInt(times);
            } catch (NumberFormatException e) {
                r.append("\n字段：LoopController.loop，%s 不是合法的整数", times);
            }
        }
        return r;
    }

    @Override
    public void execute(ContextWrapper ctx, DefaultTestResult result) {
        int loops;
        try {
            String loopsValue = ctx.evalAsString(times).trim();
            loops = Integer.parseInt(loopsValue);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(String.format("字段：LoopController.loop，%s 不是合法的整数", times));
        }

        loopCount = -1;
        VariableConfigItem variables = ctx.getAllVariablesWrapper().getLastVariableConfigItem();
        for (int i = 1; i <= loops; i++) {
            loopCount = i;
            variables.put(CURRENT_LOOP_COUNT, i);
            withTestStepNumberLog(ctx, i, () -> executeSubSteps(ctx));
        }
    }

    @Override
    protected DefaultTestResult createTestResult() {
        return new DefaultTestResult();
    }

    @Override
    protected TestStepContext createCurrentContext() {
        TestStepContext ctx = super.createCurrentContext();
        if (ctx.getConfigGroup() == null)
            ctx.setConfigGroup(new TestElementConfig());
        if (ctx.getConfigGroup().getVariableConfigItem() == null)
            ctx.getConfigGroup().put(VariableConfigItem.KEY, new VariableConfigItem());
        return ctx;
    }

    @Override
    public RepeatController copy() {
        RepeatController self = super.copy();
        self.times = times;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (LoopController)
    // ---------------------------------------------------------------------

    public String getTimes() {
        return times;
    }

    public void setTimes(String times) {
        this.times = times;
    }

    public int getLoopCount() {
        return loopCount;
    }

    // ---------------------------------------------------------------------
    // Builder (LoopController.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractContainerController.Builder<RepeatController, Builder, DefaultTestResult>
        implements TestElementBuilder<RepeatController> {

        private String loopsAsString;

        /**
         * 循环次数
         *
         * @param times 循环次数
         * @return 当前对象
         */
        public Builder times(String times) {
            this.loopsAsString = times;
            return this;
        }

        /**
         * 循环次数
         *
         * @param times 循环次数
         * @return 当前对象
         */
        public Builder times(int times) {
            this.loopsAsString = String.valueOf(times);
            return this;
        }

        @Override
        public RepeatController build() {
            return new RepeatController(this);
        }

    }

}
