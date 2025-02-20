package com.liyunx.groot.testelement.controller;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.builder.*;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.ExecuteSubStepsFilterChain;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.support.Worker;
import com.liyunx.groot.testelement.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 容器控制器抽象类：子元件控制器，负责调度子元件。
 *
 * <p>某些容器类可能不是 {@link AbstractContainerController} 子类，比如增加一个 If-Else-Controller 可能包含多个子步骤集合。
 */
@SuppressWarnings("unchecked")
public abstract class AbstractContainerController<S extends AbstractContainerController<S, T>, T extends TestResult<T>>
    extends AbstractTestElement<S, T>
    implements Controller<T>, ExecuteSubStepsFilterChain {

    private static final Logger log = LoggerFactory.getLogger(AbstractContainerController.class);

    @JSONField(name = "steps")
    protected List<TestElement<?>> steps;

    protected Iterator<TestFilter> executeSubStepsFilters;

    public AbstractContainerController() {
    }

    protected AbstractContainerController(Builder<S, ?> builder) {
        super(builder);
        this.steps = builder.steps;
    }

    @Override
    public void doExecuteSubSteps(ContextWrapper ctx) {
        if (executeSubStepsFilters.hasNext()) {
            TestFilter next = executeSubStepsFilters.next();
            next.doExecuteSubSteps(ctx, this);
        } else {
            if (steps != null) {
                for (TestElement<?> step : steps) {
                    if (step != null) {
                        ctx.getSessionRunner().run(step);
                    }
                }
            }
        }
    }

    protected void executeSubSteps(ContextWrapper contextWrapper) {
        executeSubStepsFilters = filters.iterator();
        doExecuteSubSteps(contextWrapper);
    }

    // 输出带有步骤编号的日志
    protected void withTestStepNumberLog(ContextWrapper ctx, int loopIndex, Worker worker) {
        Map<String, Object> sessionStorage = ctx.getSessionRunner().getStorage();
        Stack<Integer> testStepNumberStack = (Stack<Integer>) sessionStorage.get(TEST_STEP_NUMBER_STACK);
        testStepNumberStack.push(loopIndex);
        sessionStorage.put(TEST_STEP_NUMBER_PREVIOUS_NO, 0);
        String testStepNumber = generateTestStepNumber(testStepNumberStack);
        log.info("{} -> 第 {} 次循环", testStepNumber, loopIndex);
        worker.work();
        log.info("{} <- 第 {} 次循环", testStepNumber, loopIndex);
        testStepNumberStack.pop();
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();
        if (steps != null) {
            for (TestElement<?> step : steps) {
                r.append(step);
            }
        }
        return r;
    }

    @Override
    public S copy() {
        S self = super.copy();
        if (steps != null) {
            List<TestElement<?>> newSteps = new ArrayList<>();
            for (TestElement<?> step : steps) {
                newSteps.add(step.copy());
            }
            self.steps = newSteps;
        }
        return self;
    }

    public List<TestElement<?>> getSteps() {
        return steps;
    }

    public void setSteps(List<TestElement<?>> steps) {
        this.steps = steps;
    }


    // ---------------------------------------------------------------------
    // Builder (AbstractContainerController.Builder)
    // ---------------------------------------------------------------------

    //@formatter:off
    public static abstract class Builder<ELEMENT extends AbstractContainerController<ELEMENT, ?>,
                                         SELF extends Builder<ELEMENT, SELF>>
        extends AbstractTestElement.Builder<ELEMENT, SELF,
                                            AllConfigBuilder,
                                            CommonPreProcessorsBuilder,
                                            CommonPostProcessorsBuilder, CommonExtractorsBuilder, CommonAssertionsBuilder>
    //@formatter:on
    {

        protected List<TestElement<?>> steps = new ArrayList<>();

        // ---------------------------------------------------------------------
        // 获取具体的 Builder 对象
        // ---------------------------------------------------------------------

        @Override
        protected AllConfigBuilder getConfigBuilder() {
            return new AllConfigBuilder();
        }

        @Override
        protected CommonPreProcessorsBuilder getSetupBuilder() {
            return new CommonPreProcessorsBuilder();
        }

        @Override
        protected CommonExtractorsBuilder getExtractBuilder() {
            return new CommonExtractorsBuilder();
        }

        @Override
        protected CommonAssertionsBuilder getAssertBuilder() {
            return new CommonAssertionsBuilder();
        }

        @Override
        protected CommonPostProcessorsBuilder getTeardownBuilder() {
            return new CommonPostProcessorsBuilder(this);
        }

        // ---------------------------------------------------------------------
        // 嵌套的子步骤构建
        // ---------------------------------------------------------------------

        public SELF steps(Worker steps) {
            this.steps = List.of(new BridgeTestElement(steps));
            return self;
        }

        /**
         * 子步骤
         *
         * @param builders 子步骤 Builder（可以多个）
         * @return 当前对象
         */
        public SELF steps(TestElementBuilder<?>... builders) {
            steps.clear();
            if (builders != null) {
                for (TestElementBuilder<?> builder : builders) {
                    if (builder != null) {
                        steps.add(builder.build());
                    }
                }
            }
            return self;
        }

        /**
         * 子步骤
         *
         * @param builders 子步骤 Builder（可以多个）
         * @return 当前对象
         */
        public SELF steps(List<TestElementBuilder<?>> builders) {
            steps.clear();
            if (builders != null) {
                for (TestElementBuilder<?> builder : builders) {
                    if (builder != null) {
                        steps.add(builder.build());
                    }
                }
            }
            return self;
        }

        /**
         * 子步骤（追加单个步骤）
         *
         * @param builder 子步骤 Builder
         * @return 当前对象
         */
        public SELF step(TestElementBuilder<?> builder) {
            if (builder != null) {
                steps.add(builder.build());
            }
            return self;
        }

        /**
         * 子步骤（追加单个步骤）
         *
         * @param step 子步骤
         * @return 当前对象
         */
        public SELF step(TestElement<?> step) {
            if (step != null) {
                steps.add(step);
            }
            return self;
        }

    }

}
