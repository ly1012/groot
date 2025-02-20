package com.liyunx.groot.testng.listener;

import com.liyunx.groot.support.TraceableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCaseLogListenerTest {

    private static final Logger logger = LoggerFactory.getLogger(TestCaseLogListenerTest.class);

    @Test
    public void testAsyncTask(Method method) throws InterruptedException, ExecutionException {
        String methodName = method.getDeclaringClass().getName() + "." +method.getName();
        File logFile = new File("logs/case/" + methodName + ".0.log");
        if (logFile.exists() && logFile.isFile()) {
            assert logFile.delete();
        }

        logger.info("testAsync Start");
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(TraceableTask.wrap(() -> {
            logger.info("async task 1 execute");
        }));
        Future<String> future = executorService.submit(TraceableTask.wrap(() -> {
            logger.info("async task 2 execute");
            return "successful";
        }));
        logger.info("async task 2 result: {}", future.get());
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);

        assertThat(logFile).exists();
    }

}
