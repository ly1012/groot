package com.liyunx.groot.testng;

import com.liyunx.groot.Groot;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.TestRunner;
import com.liyunx.groot.testng.annotation.GrootSupport;
import com.liyunx.groot.testng.support.DataProviderMethodProxy;
import org.testng.IDataProviderMethod;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.internal.TestNGMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.liyunx.groot.util.AnnotationUtil.hasAnnotation;

/**
 * Groot TestNG 测试用例抽象类，子类可根据需要重写和实现某些方法
 */
public abstract class AbstractGrootTestNGTestCase {

    private static final String TESTNG_TEST_METHOD_PARAMS = "TESTNG_TEST_METHOD_PARAMS";

    private final Map<IDataProviderMethod, TestRunner> testRunnerStorage = new HashMap<>();

    @BeforeMethod(alwaysRun = true)
    public void createSessionAndSetAndStart(ITestResult testResult) {
        // 有 @GrootSupport 注解的才会创建 session
        if (!isGrootSupport(testResult)) {
            return;
        }

        Groot groot = getGrootInstance();

        TestRunner testRunner;
        ITestNGMethod testMethod = testResult.getMethod();
        IDataProviderMethod dataProviderMethod = testMethod.getDataProviderMethod();
        //*
        // 未使用 @DataProvider，直接实例化一个新对象
        if (dataProviderMethod == null) {
            testRunner = groot.newTestRunner();
        }
        // 使用了 @DataProvider，则共享 TestRunner 对象
        // 假设 @Test(invocationCount = 3) 且 @DataProvider 返回三行数据，则创建如下对象：
        // invocation   TestRunner   SessionRunner
        // 1            t1           s11, s12, s13
        // 2            t2           s21, s22, s23
        // 3            t3           s31, s32, s33
        // 即 @Test(invocationCount = n) 会创建 n 个 TestRunner 对象
        else {
            // 使用代理对象存储 TestRunner 对象，用完即释放
            if (testMethod instanceof TestNGMethod _testMethod) {
                synchronized (dataProviderMethod) {
                    dataProviderMethod = testMethod.getDataProviderMethod();
                    if (dataProviderMethod instanceof DataProviderMethodProxy) {
                        testRunner = ((DataProviderMethodProxy) dataProviderMethod).getTestRunner();
                    } else {
                        testRunner = groot.newTestRunner();
                        _testMethod.setDataProviderMethod(new DataProviderMethodProxy(dataProviderMethod, testRunner));
                    }
                }
            }
            // 兜底方案：提升到 Class 级别存储 TestRunner 对象，更晚释放存活时间更长，可能会占用更多内存
            else {
                synchronized (dataProviderMethod) {
                    testRunner = testRunnerStorage.get(dataProviderMethod);
                    if (testRunner == null) {
                        testRunner = groot.newTestRunner();
                        testRunnerStorage.put(dataProviderMethod, testRunner);
                    }
                }
            }
        }
        //*/

        SessionRunner session = testRunner.newSessionRunner();
        SessionRunner.setSession(session);
        Object[] parameters = testResult.getParameters();
        if (parameters != null && parameters.length == 1) {
            Object param = parameters[0];
            if (param instanceof Map _param) {
                session.getStorage().put(TESTNG_TEST_METHOD_PARAMS, param);
                session.config(config -> config.variables(_param));
            }
        }
        startSession(session, testResult);
    }

    @AfterMethod(alwaysRun = true)
    public void stopSessionAndRemove(ITestResult testResult) {
        SessionRunner session = SessionRunner.getSession();
        if (session != null) {
            stopSession(session, testResult);
        }
        SessionRunner.removeSession();
    }

    @AfterClass(alwaysRun = true)
    public void clearTestRunnerStorage() {
        testRunnerStorage.clear();
    }

    /**
     * 读取当前测试方法参数
     *
     * <p>仅适用于 @Test 方法且需要在 @BeforeMethod 中通过 SessionRunner 存储了方法参数（Map）</p>
     *
     * @param key 参数名
     * @param <T> 参数类型
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    protected <T> T arg(String key) {
        if (key == null) {
            throw new IllegalArgumentException("参数不能为 null");
        }

        SessionRunner session = SessionRunner.getSession();
        if (session == null) {
            throw new IllegalStateException("SessionRunner 为 null");
        }

        Object params = session.getStorage().get(TESTNG_TEST_METHOD_PARAMS);
        if (params == null) {
            throw new IllegalStateException("未找到当前 @Test 方法的参数");
        }
        if (!(params instanceof Map)) {
            throw new IllegalStateException("当前仅支持 Map 类型的方法参数");
        }

        Map<String, ?> map = (Map<String, ?>) params;
        return (T) map.get(key);
    }

    /**
     * 获取 Groot 实例，用于 @Test 方法执行前创建 SessionRunner 对象。
     *
     * <p>可能的实例化时机（举几个例子）：
     * <ul>
     *     <li>全局单例：<code>@BeforeSuite + static Groot</code></li>
     *     <li>每个环境仅有一个实例：<code>@BeforeTest + static Map&lt;String(envName), Groot&gt;</code></li>
     *     <li>某个 Class 使用独立的实例：<code>@BeforeClass + Groot</code></li>
     *     <li>每次方法执行前都创建新的实例</li>
     * </ul></p>
     *
     * <p>调用时机：@BeforeMethod</p>
     *
     * @return Groot 实例
     */
    protected abstract Groot getGrootInstance();

    /**
     * 子类可以重写该方法，比如在 start 前增加一些 Selenium 相关配置
     *
     * <p>该方法被 @BeforeMethod {@link #createSessionAndSetAndStart(ITestResult)} 调用</p>
     *
     * @param session    @Test 方法本次调用期间使用的 SessionRunner 对象
     * @param testResult @Test 方法本次调用的上下文对象
     */
    protected void startSession(SessionRunner session, ITestResult testResult) {
        session.start();
    }

    /**
     * 子类可以重写该方法
     *
     * <p>该方法被 @AfterMethod {@link #stopSessionAndRemove(ITestResult)} 调用</p>
     *
     * @param session    @Test 方法本次调用期间使用的 SessionRunner 对象
     * @param testResult @Test 方法本次调用的上下文对象
     */
    protected void stopSession(SessionRunner session, ITestResult testResult) {
        session.stop();
    }

    private boolean isGrootSupport(ITestResult testResult) {
        Method method = testResult.getMethod().getConstructorOrMethod().getMethod();
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            if (hasAnnotation(annotation.annotationType(), GrootSupport.class)) {
                return true;
            }
        }
        return false;
    }

}
