package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.testelement.TestElementBuilder;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.DefaultTestResult;

/**
 * 简单控制器，没有额外的控制逻辑，可用于分组或增加一层变量作用域等场景。
 */
@KeyWord(GroupController.KEY)
public class GroupController extends AbstractContainerController<GroupController, DefaultTestResult> {

    public static final String KEY = "group";

    public GroupController() {
    }

    private GroupController(Builder builder) {
        super(builder);
    }

    @Override
    protected DefaultTestResult createTestResult() {
        return new DefaultTestResult();
    }

    @Override
    protected void execute(ContextWrapper ctx, DefaultTestResult result) {
        executeSubSteps(ctx);
    }

    // ---------------------------------------------------------------------
    // Builder (SimpleController.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractContainerController.Builder<GroupController, Builder, DefaultTestResult>
        implements TestElementBuilder<GroupController> {

        @Override
        public GroupController build() {
            return new GroupController(this);
        }
    }

}
