package com.liyunx.groot.support;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 线程任务包装类，使用 Capture/Replay 模型解决异步日志跟踪问题
 */
public class TraceableTask {

    public static Runnable wrap(Runnable task) {
        return new TraceableRunnable(task);
    }

    public static <V> Callable<V> wrap(Callable<V> task) {
        return new TraceableCallable<>(task);
    }


    static class TraceableRunnable implements Runnable {

        private final Map<String, String> mdcContext;
        private final Runnable task;

        public TraceableRunnable(Runnable task) {
            // Capture：获取当前线程的 MDC 上下文快照
            this.mdcContext = MDC.getCopyOfContextMap();
            this.task = task;
        }

        public TraceableRunnable(Map<String, String> mdcContext, Runnable task) {
            this.mdcContext = mdcContext;
            this.task = task;
        }

        @Override
        public void run() {
            try {
                // Replay：将父线程 MDC 上下文设置到当前线程（子线程）
                MDC.setContextMap(mdcContext);
                task.run();
            } finally {
                // 由于线程池会重复使用线程，每个异步任务结束后需要清理掉当前线程的 MDC 上下文
                MDC.clear();
            }
        }
    }

    static class TraceableCallable<V> implements Callable<V> {

        private final Map<String, String> mdcContext;
        private final Callable<V> task;

        public TraceableCallable(Callable<V> task) {
            // Capture：获取当前线程的 MDC 上下文快照
            this.mdcContext = MDC.getCopyOfContextMap();
            this.task = task;
        }

        public TraceableCallable(Map<String, String> mdcContext, Callable<V> task) {
            this.mdcContext = mdcContext;
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            try {
                // Replay：将父线程 MDC 上下文设置到当前线程（子线程）
                MDC.setContextMap(mdcContext);
                return task.call();
            } finally {
                // 由于线程池会重复使用线程，每个异步任务结束后需要清理掉当前线程的 MDC 上下文
                MDC.clear();
            }
        }
    }

}
