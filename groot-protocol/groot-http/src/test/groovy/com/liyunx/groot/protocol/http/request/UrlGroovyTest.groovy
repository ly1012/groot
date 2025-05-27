package com.liyunx.groot.protocol.http.request

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith

class UrlGroovyTest extends WireMockTestNGTestCase {

    @Test(description = "Request URL: BaseUrl 和 Url 测试")
    public void testUrl_BaseUrl() {
        sessionConfig {
            variables {
                var "httpPort", httpPort
            }
        }

        WireMock.stubFor(WireMock
            .get("/get/123")
            .willReturn(WireMock.ok()))

        httpWith("baseUrl 覆盖") {
            request {
                baseUrl "http://localhost:\${httpPort}/get"
                get "/123"
            }
            validate {
                statusCode 200
            }
        }

        httpWith("url 使用绝对路径") {
            request {
                get "http://localhost:\${httpPort}/get/123"
            }
            validate {
                statusCode 200
            }
        }
    }

    @Test(description = "Request URL 路径变量")
    public void testUrl_PathVariables() {
        WireMock.stubFor(WireMock
            .get("/get/123/detail")
            .willReturn(WireMock.ok()));

        httpWith("路径变量测试") {
            variables {
                var "method", "get"
            }
            request {
                get '/${method}/:id/detail'
                pathVariable "id", "123"
            }
            validate {
                statusCode 200
            }
        }
    }

    @Test(description = "Request URL 查询参数")
    public void testUrl_QueryParams() {
        WireMock.stubFor(WireMock
            .get(WireMock.urlPathEqualTo("/get"))
            .withQueryParam("lang", WireMock.equalTo("java"))
            .withQueryParam("name", WireMock.havingExactly("java", "groot"))
            .withQueryParam("age", WireMock.equalTo("18"))
            .willReturn(WireMock.ok()));

        httpWith("查询参数测试") {
            request {
                get "/get?lang=java&name=java"
                queryParam "name", "groot"
                queryParam "age", "18"
            }
            validate {
                statusCode 200
            }
        }

        httpWith("查询参数多值测试") {
            request {
                get "/get?lang=java&age=18"
                queryParam "name", "java", "groot"
            }
            validate {
                statusCode 200
            }
        }
    }


}
