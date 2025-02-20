package com.liyunx.groot.protocol.http.yaml;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.SessionRunner.getSession;

public class UrlTest extends WireMockTestNGTestCase {

    @Test(description = "Request URL 路径变量")
    public void testUrl_PathVariables() {
        WireMock.stubFor(WireMock
            .get("/get/123/detail")
            .willReturn(WireMock.ok()));

        getSession().run("testcases/url/pathVariables.yml");
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
    }

}
