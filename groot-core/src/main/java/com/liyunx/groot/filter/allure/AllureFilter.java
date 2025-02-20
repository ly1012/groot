package com.liyunx.groot.filter.allure;

import com.liyunx.groot.common.Ordered;
import com.liyunx.groot.common.Unique;
import com.liyunx.groot.filter.TestFilter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.qameta.allure.Allure.getLifecycle;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Allure 监听器，和 <a href="https://github.com/allure-framework/allure2">Allure</a> 测试报告集成。
 */
public interface AllureFilter extends TestFilter, Ordered, Unique {

    String ALLURE_FILTER_CLASS_NAME = AllureFilter.class.getName();

    @Override
    default int getOrder() {
        return Ordered.DEFAULT_PRECEDENCE;
    }

    /**
     * 一个 TestElement 对象最多只能持有一个 AllureFilter 实例
     */
    @Override
    default String uniqueId() {
        return ALLURE_FILTER_CLASS_NAME;
    }

    /**
     * 功能上相当于 <code>Allure.step(...)</code>
     *
     * @param nameSupplier 步骤名称
     * @param code         步骤开始和结束之间的代码
     */
    static void step(Supplier<String> nameSupplier, Consumer<String> code) {
        step(nameSupplier, () -> Status.PASSED, code);
    }

    /**
     * 功能上相当于 <code>Allure.step(...)</code>
     *
     * @param nameSupplier   步骤名称
     * @param statusSupplier code 成功执行后的状态
     * @param code           步骤开始和结束之间的代码
     */
    static void step(Supplier<String> nameSupplier, Supplier<Status> statusSupplier, Consumer<String> code) {
        String uuid = UUID.randomUUID().toString();
        getLifecycle().startStep(uuid, new StepResult().setName(nameSupplier.get()));
        try {
            code.accept(uuid);
            getLifecycle().updateStep(uuid, stepResult -> stepResult
                .setName(nameSupplier.get())
                .setStatus(statusSupplier.get()));
        } catch (Throwable throwable) {
            getLifecycle().updateStep(uuid, stepResult -> stepResult
                .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                .setStatusDetails(getStatusDetails(throwable).orElse(null)));
            throw throwable;
        } finally {
            getLifecycle().stopStep(uuid);
        }
    }

}
