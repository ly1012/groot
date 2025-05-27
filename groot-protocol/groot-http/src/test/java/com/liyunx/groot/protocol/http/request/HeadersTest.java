package com.liyunx.groot.protocol.http.request;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;

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

        http("Header Name 不重复", request -> request
            .get("/headers")
            .headers(
                "X-Token", "Z3Jvb3Q=",
                "OrderId", "872160725"
            ));

        http("Header Name 重复", request -> request
            .get("/headers/duplicate")
            .headers(
                "X-Token", "Z3Jvb3Q=",
                "ids", "166",
                "ids", "288"
            ));

        httpWith("Header 值包含表达式", action -> action
            .variables(variables -> variables
                .var("token", "Z3Jvb3Q=")
                .var("startValue", "872"))
            .request(request -> request
                .get("/headers")
                .headers(
                    "X-Token", "${token}",
                    "OrderId", "${startValue}160725"
                )));
    }

    @Test(description = "Request Header Cookies 测试")
    public void testHeaders_Cookies() {
        WireMock.stubFor(WireMock
            .get("/headers/cookies")
            .withCookie("kv1", WireMock.equalTo("value1"))
            .withCookie("kv2", WireMock.equalTo("value2"))
            .willReturn(WireMock.ok()));

        getSession().run("testcases/headers/cookies.yml");

        http("Cookies 测试", request -> request
            .get("/headers/cookies")
            .headers("Cookie", "kv1=111111; kv2=value2")
            .cookies("kv1", "value1"));

        httpWith("Cookie 值包含表达式", action -> action
            .variables(variables -> variables
                .var("kv1", "value1")
                .var("kv2", "value2"))
            .request(request -> request
                .get("/headers/cookies")
                .cookies(
                    "kv1", "${kv1}",
                    "kv2", "${kv2}"
                )));
    }

}
