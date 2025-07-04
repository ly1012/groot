package com.liyunx.groot.protocol.http

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.protocol.http.annotation.DelegatesToHttpSamplerBuilder
import com.liyunx.groot.protocol.http.support.HttpSupport
import org.testng.annotations.Test

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith
import static org.assertj.core.api.Assertions.assertThat

class HttpVirtualRunnerGroovyTest extends WireMockTestNGTestCase {

    @Test
    void testConfigSelf() {
        WireMock.stubFor(WireMock
            .post("/post/GROOT")
            .willReturn(WireMock
                .ok("T O O R G")))

        def moneyValue
        httpWith("xxx") {
            configSelf {
                header "money", "many"
            }
            config {
                http {
                    anyService {
                        header "money", "many many"
                        header "dollar", "yuan"
                    }
                }
            }
            request {
                post '/post/:groot'
                pathVariable 'groot', 'GROOT'
                body 'G R O O T'
            }
            teardown {
                validate {
                    equalTo r.request.headers.get("dollar"), "yuan"
                    moneyValue = "many many"
                }
            }
            validate {
                statusCode 200
            }
            validate {
                equalTo r.request.headers.get("money"), moneyValue
            }
        }
    }

    @Test
    void testHttp() {
        WireMock.stubFor(WireMock
            .post("/post/GROOT")
            .willReturn(WireMock
                .ok("T O O R G")))

        http('http test') {
            post '/post/:groot'
            pathVariable 'groot', 'GROOT'
            body 'G R O O T'
        }.then {
            assertThat(it.response.body).isEqualTo("T O O R G")
        }
    }

    @Test
    void testAPIObject() {
        WireMock.stubFor(WireMock
            .post("/post/GROOT")
            .willReturn(WireMock
                .ok("T O O R G")))

        // 直接调用
        bizApi("GROOT")

        // 使用前后置
        bizApi("GROOT") {
            validate {
                statusCode 200
            }
        }
    }

    // 业务接口封装
    static HttpSampleResult bizApi(String groot, @DelegatesToHttpSamplerBuilder Closure... closure) {
        def closures = HttpSupport.mergeClosures({
            validate {
                statusCode 200
            }
        }, closure, null)
        return httpWith("闭包测试", closures) {
            post '/post/:groot'
            pathVariable 'groot', groot
            body 'G R O O T'
        }
    }



}


