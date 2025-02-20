package com.liyunx.groot.examples.perf;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.template.freemarker.FreeMarkerTemplateEngine;
import com.liyunx.groot.util.YamlUtil;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * 第三方库初始化/首次调用耗时测试
 * Logback Init：115 ms
 * Yaml Init：18 ms
 * FastJSON2 Init：53 ms
 * FreeMarkerTemplateEngine Init：71 ms
 * OkHttp Init：91 ms
 * SPI Init：34 ms
 * Total Time: ：384 ms
 */
public class InitLoadPerformanceTest {

    public static void main(String[] args) {
        test("Total Time: ", InitLoadPerformanceTest::testInitLoadPerformance);
    }

    @Test(enabled = false)
    public static void testInitLoadPerformance() {
        test("Logback Init", () -> {
            Logger log = LoggerFactory.getLogger(InitLoadPerformanceTest.class);
            log.info("log info statement");
        });

        test("Yaml Init", () -> {
            YamlUtil.getYaml().load("");
            YamlUtil.getYaml().dump(new HashMap<>());
        });

        test("FastJSON2 Init", () -> {
            JSON.parseObject("{}", HashMap.class);
            JSON.toJSONString(new HashMap<>());
        });

        test("FreeMarkerTemplateEngine Init", () -> {
            new FreeMarkerTemplateEngine().eval(new HashMap<>(), "${1+1}");
        });

        test("OkHttp Init", () -> {
            new OkHttpClient.Builder().build();
        });

        test("SPI Init", () -> {
            ApplicationConfig.getFunctions();
            ApplicationConfig.getFastJson2Interceptors();
            ApplicationConfig.getFileDataLoaders();
            ApplicationConfig.getAllureFilters();

            ApplicationConfig.getGrootListeners();
            ApplicationConfig.getTestRunnerListeners();
            ApplicationConfig.getSessionRunnerListeners();
            ApplicationConfig.getSessionRunnerInheritances();

            ApplicationConfig.getTestElementKeyMap();
            ApplicationConfig.getConfigItemKeyMap();
            ApplicationConfig.getPreProcessorKeyMap();
            ApplicationConfig.getPostProcessorKeyMap();
            ApplicationConfig.getExtractorKeyMap();
            ApplicationConfig.getAssertionKeyMap();
            ApplicationConfig.getTestFilterKeyMap();
            ApplicationConfig.getMappingKeyMap();
            ApplicationConfig.getAssertionValueTypeMap();
        });
    }

    private static void test(String name, Runnable runnable) {
        long s = System.currentTimeMillis();
        runnable.run();
        long e = System.currentTimeMillis();
        System.out.println(name + "：" + (e - s) + " ms");
    }

}
