package com.liyunx.groot.examples.httpbin;

import com.liyunx.groot.testng.GrootTestNGTestCase;
import com.liyunx.groot.testng.annotation.GrootSupport;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;
import static org.hamcrest.Matchers.containsString;

public class PostTest extends GrootTestNGTestCase {

    @BeforeMethod
    public void setUp() {
        // 一般情况下，在 global.yml 或 env-test.yml 配置即可
        // 因为本项目为演示项目，所以这里手动配置，覆盖环境和全局配置，防止多个用例冲突
        sessionConfig(config -> config
            .http(http -> http
                .anyService(any -> any
                    .baseUrl("https://httpbin.org"))));
    }

    @GrootSupport
    @Test
    public void testPost() {
        http("上传文件并保存响应到指定文件（直接请求）", request -> request
            .post("/anything/:id")
            .pathVariable("id", "china")
            .header("myHeader", "myValue")
            .multiPartFile("data/中文.pdf")
            .download("download/上传文件.txt"));

        httpWith("上传文件并保存响应到指定文件（请求并执行配置或前置或后置动作）", action -> action
            .request(request -> request
                .post("/anything/:id")
                .pathVariable("id", "china")
                .header("myHeader", "myValue")
                .multiPartFile("data/中文.pdf")
                .download("download/上传文件.txt"))
            .teardown(teardown -> teardown
                // 提取前先断言，如果请求失败直接结束用例
                .validate(validate -> validate
                    .statusCode(200))
                .extract(extract -> extract
                    .jsonpath("realRequestUrl", "$.url"))
                // 提取后继续断言
                .validate(validate -> validate
                    .equalTo("${realRequestUrl}", "https://httpbin.org/anything/china")
                    .body(containsString("https://httpbin.org/anything/china")))));
    }

}
