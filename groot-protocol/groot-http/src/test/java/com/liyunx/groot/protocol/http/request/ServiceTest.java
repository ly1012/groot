package com.liyunx.groot.protocol.http.request;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;

public class ServiceTest extends WireMockTestNGTestCase {

    @Test(description = "Request URL 路径变量")
    public void testUrl_PathVariables() {
        wireMockServer.stubFor(WireMock
            .get("/user/info")
            .willReturn(WireMock.ok()
                .withHeader("ServiceName", "{{request.headers.ServiceName}}")
                .withTransformers("response-template")));

        getSession().run("testcases/service/user_service.yml");

        sessionConfig(config -> config
            .http(http -> http
                .anyService(any -> any
                    .header("ServiceName", "anyService"))
                .service("userService", service -> service
                    .header("ServiceName", "userService"))));
        httpWith("指定 Service 测试", action -> action
            .request(request -> request
                .get("/user/info")
                .withService("userService"))
            .validate(validate -> validate
                .statusCode(200)
                .header("ServiceName", "userService")));
    }

}
