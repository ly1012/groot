package com.liyunx.groot.support;

import org.slf4j.MDC;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceableTaskTest {

    @Test
    public void testWrapRunnable() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // 使用 TraceableTask.wrap 包装任务
        Ref<String> outer1 = Ref.ref();
        MDC.setContextMap(Map.of("outer1", "out"));
        Future<?> future1 = executorService.submit(TraceableTask.wrap(() -> {
            outer1.value = MDC.get("outer1");
        }));
        future1.get();
        assertThat(outer1.value).isEqualTo("out");

        // 不使用 TraceableTask.wrap 包装任务，MDC 上下文不会被传递到子线程
        Ref<String> outer2 = Ref.ref();
        MDC.setContextMap(Map.of("outer2", "out"));
        Future<?> future2 = executorService.submit(() -> {
            outer2.value = MDC.get("outer2");
        });
        future2.get();
        assertThat(outer2.value).isNull();
    }

    @Test
    public void testWrapCallable() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // 使用 TraceableTask.wrap 包装任务
        Ref<String> outer1 = Ref.ref();
        MDC.setContextMap(Map.of("outer1", "out"));
        Future<?> future1 = executorService.submit(TraceableTask.wrap(() -> {
            outer1.value = MDC.get("outer1");
            return "out1";
        }));
        assertThat(future1.get()).isEqualTo("out1");
        assertThat(outer1.value).isEqualTo("out");

        // 不使用 TraceableTask.wrap 包装任务，MDC 上下文不会被传递到子线程
        Ref<String> outer2 = Ref.ref();
        MDC.setContextMap(Map.of("outer2", "out"));
        Future<?> future2 = executorService.submit(() -> {
            outer2.value = MDC.get("outer2");
            return "out2";
        });
        assertThat(future2.get()).isEqualTo("out2");
        assertThat(outer2.value).isNull();
    }

}