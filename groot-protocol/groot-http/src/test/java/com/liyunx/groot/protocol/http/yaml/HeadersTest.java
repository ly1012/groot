package com.liyunx.groot.protocol.http.yaml;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;

public class HeadersTest extends WireMockTestNGTestCase {

    @Test(description = "Request Headers 测试")
    public void testHeaders() {
        WireMock.stubFor(WireMock
            .get("/headers")
            .withHeader("X-Token", WireMock.equalTo("Z3Jvb3Q="))
            .withHeader("OrderId", WireMock.equalTo("872160725"))
            .willReturn(WireMock.ok()));

        WireMock.stubFor(WireMock
            .get("/headers/duplicate")
            .withHeader("X-Token", WireMock.equalTo("Z3Jvb3Q="))
            .withHeader("ids", WireMock.havingExactly("166", "288"))
            .willReturn(WireMock.ok()));

        getSession().run("testcases/headers/headers.yml");
    }

    @Test(description = "Request Header Cookies 测试")
    public void testHeaders_Cookies() {
        WireMock.stubFor(WireMock
            .get("/headers/cookies")
            .withCookie("kv1", WireMock.equalTo("value1"))
            .withCookie("kv2", WireMock.equalTo("value2"))
            .willReturn(WireMock.ok()));

        getSession().run("testcases/headers/cookies.yml");
    }

}
