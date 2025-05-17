package com.liyunx.groot.protocol.http.filter

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.nio.charset.StandardCharsets

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig
import static com.liyunx.groot.SessionRunner.getSession
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith

class CustomFilterTest extends WireMockTestNGTestCase {

    @BeforeMethod
    void beforeMethod() {
        wireMockServer.stubFor(WireMock
            .post(WireMock
                .urlPathTemplate("/safe/api/{id}"))
            .andMatching(req -> {
                String origin = base64Decode(req.bodyAsString)
                return MatchResult.of(origin == "request data " + req.pathParameters.get("id"))
            })
            .willReturn(WireMock
                .okJson("{{base64 (stringFormat 'response data %s' request.pathSegments.2)}}")
                .withTransformers("response-template")))
    }

    @Test
    public void testCustomFilter() {
        // 针对本用例的所有 HttpSampler 请求应用加解密插件
        sessionConfig {
            filters new BankServiceEncryptDecryptFilter()
        }

        httpWith("加解密测试：请求六六六") {
            request {
                post "/safe/api/666"
                body "request data 666"
            }
            validate {
                equalTo('${r.request.body}', this.base64Encode("request data 666"))
                equalTo('${r.response.body}', "response data 666")
            }
        }

        httpWith("加解密测试：请求八八八") {
            request {
                post "/safe/api/888"
                body "request data 888"
            }
            validate {
                equalTo('${r.request.body}', this.base64Encode("request data 888"))
                equalTo('${r.response.body}', "response data 888")
            }
        }

        httpWith("加解密测试：请求一一一") {
            config {
                filters new BankServiceEncryptDecryptFilter2("123456")
            }
            request {
                post "/safe/api/111"
                body "request data 111"
            }
            validate {
                equalTo('${r.request.body}', this.base64Encode("request data 111"))
                equalTo('${r.response.body}', "response data 111")
                equalTo('${r.response.headers.encryptKey}', "123456")
            }
        }
    }

    @Test
    public void testCustomFilterByYaml() {
        getSession().run("testcases/filter/encrypt_decrypt_filter.yml")
    }

    @Test
    public void testCustomFilter2ByYaml() {
        getSession().run("testcases/filter/encrypt_decrypt_filter2.yml")
    }

    public static String base64Encode(String input) {
        return Base64.encoder.encodeToString(input.getBytes(StandardCharsets.UTF_8))
    }

    public static String base64Decode(String input) {
        return new String(Base64.decoder.decode(input.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
    }
}
