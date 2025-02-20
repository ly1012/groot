package com.liyunx.groot.protocol.http;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.functions.Function;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.Groot;
import com.liyunx.groot.SessionRunner;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.util.List;
import java.util.function.BiFunction;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.SessionRunner.*;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * WireMock TestNG 测试基类
 */
public abstract class WireMockTestNGTestCase {

    private static WireMockServer wireMockServer;
    protected static int httpPort;
    protected static int httpsPort;
    protected static Groot groot;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        // 禁用 Jetty 服务器日志
        // from https://stackoverflow.com/questions/2120370/jetty-how-to-disable-logging
        //System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        //System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        //org.eclipse.jetty.util.log.StdErrLog logger = new org.eclipse.jetty.util.log.StdErrLog();
        //logger.setLevel(org.eclipse.jetty.util.log.StdErrLog.LEVEL_OFF);
        //org.eclipse.jetty.util.log.Log.setLog(logger);

        // 启动 Wiremock Server
        wireMockServer = new WireMockServer(
            options()
                .dynamicPort()
                .dynamicHttpsPort()
                .disableRequestJournal()
                .stubRequestLoggingDisabled(true)
                .withRootDirectory("src/test/resources/wiremock")
        );
        wireMockServer.start();
        // 配置 WireMock Client
        httpPort = wireMockServer.port();
        httpsPort = wireMockServer.httpsPort();
        configureFor(httpPort);
        // Groot Runner
        groot = new Groot();
    }

    @BeforeMethod
    public void setUp() {
        SessionRunner session = groot.newTestRunner().newSessionRunner();
        setSession(session);
        sessionConfig(config -> config
            .http(http -> http
                .anyService(any -> any
                    .baseUrl(baseUrlWithHttpAndLocalHost()))));
        session.start();

        // 每次用例执行前重置 Mock 数据为空，不支持用例并发执行，目前用例较少，所以不考虑用例并发执行
        // 后续如果需要用例并发（测试方法级别并发），有几个方案：
        // 1. 借助 ThreadLocal，每个线程单独的 WireMock Server 和 Client（WireMock.configureFor 本身就是用的 ThreadLocal 不需要修改）
        //    服务在 BeforeMethod 中启动（只创建一次，并注册），在 AfterSuite 中统一停止。
        // 2. Mock 数据提前准备，测试用例只是请求调用，不新增 Mock 数据
        // 3. Mock 数据软隔离，每个用例新增和请求 Mock 数据时，根据 caseId Header 区分（如果数据较多，可以用例结束后删除）
        reset();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        SessionRunner session = getSession();
        if (session != null) {
            session.stop();
        }
        removeSession();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        // 停止 Wiremock 服务
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    protected String baseUrlWithHttpAndLocalHost() {
        return "http://localhost:" + httpPort;
    }

    /**
     * 自签名证书，Https 请求时需要关闭 Https 证书校验
     *
     * @return baseUrl
     */
    protected String baseUrlWithHttpsAndLocalHost() {
        return "https://localhost:" + httpsPort;
    }

    // 只增加函数，不减少函数，也不能重新赋值函数列表，防止多个单测用例并发测试失败
    protected static void addFunction(String functionName,
                                    BiFunction<ContextWrapper, List<Object>, Object> function) {
        List<Function> functions = ApplicationConfig.getFunctions();
        synchronized (functions) {
            functions.add(new Function() {
                @Override
                public String getName() {
                    return functionName;
                }

                @Override
                public Object execute(ContextWrapper contextWrapper, List<Object> parameters) {
                    return function.apply(contextWrapper, parameters);
                }
            });
        }
    }

}
