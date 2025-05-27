package com.liyunx.groot.protocol.http.request;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;

public class UrlTest extends WireMockTestNGTestCase {

    @Test(description = "Request URL 路径变量")
    public void testUrl_PathVariables() {
        WireMock.stubFor(WireMock
            .get("/get/123/detail")
            .willReturn(WireMock.ok()));

        getSession().run("testcases/url/pathVariables.yml");

        httpWith("路径变量测试", action -> action
            .variables(variables -> variables
                .var("method", "get"))
            .request(request -> request
                .get("/${method}/:id/detail")
                .pathVariable("id", "123"))
            .validate(validate -> validate
                .statusCode(200)));
    }

    @Test(description = "Request URL 查询参数")
    public void testUrl_QueryParams() {
        WireMock.stubFor(WireMock
            .get(WireMock.urlPathEqualTo("/get"))
            .withQueryParam("lang", WireMock.equalTo("java"))
            .withQueryParam("name", WireMock.havingExactly("java", "groot"))
            .withQueryParam("age", WireMock.equalTo("18"))
            .willReturn(WireMock.ok()));

        getSession().run("testcases/url/queryParams.yml");

        httpWith("查询参数测试", action -> action
            .request(request -> request
                .get("/get?lang=java&name=java")
                .queryParam("name", "groot")
                .queryParam("age", "18"))
            .validate(validate -> validate
                .statusCode(200)));

        httpWith("查询参数多值测试", action -> action
            .request(request -> request
                .get("/get?lang=java&age=18")
                .queryParam("name", "java", "groot"))
            .validate(validate -> validate
                .statusCode(200)));
    }

    @Test(description = "Request URL: BaseUrl 和 Url 测试")
    public void testUrl_BaseUrl() {
        sessionConfig(config -> config
            .variables(variables -> variables
                .var("httpPort", httpPort)));

        WireMock.stubFor(WireMock
            .get("/get/123")
            .willReturn(WireMock.ok()));

        getSession().run("testcases/url/url.yml");

        httpWith("baseUrl 覆盖", action -> action
            .request(request -> request
                .baseUrl("http://localhost:${httpPort}/get")
                .get("/123"))
            .validate(validate -> validate
                .statusCode(200)));

        httpWith("url 使用绝对路径", action -> action
            .request(request -> request
                .get("http://localhost:${httpPort}/get/123"))
            .validate(validate -> validate
                .statusCode(200)));
    }

}
