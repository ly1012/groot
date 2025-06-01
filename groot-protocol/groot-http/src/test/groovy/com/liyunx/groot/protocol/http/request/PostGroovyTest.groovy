package com.liyunx.groot.protocol.http.request

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import com.liyunx.groot.protocol.http.constants.HttpHeader
import org.assertj.core.api.Assertions
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.sv
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith

class PostGroovyTest extends WireMockTestNGTestCase {

    @Test(description = "MultiPart：上传多个文件")
    void testMultiPart_MultiFile() {
        String url = "/multipart/upload";
        String contentDispositionValue1 = "form-data; name=\"file\"; filename=\"降龙十八掌.txt\"";
        String contentDispositionValue2 = "form-data; name=\"file\"; filename=\"独孤九剑.txt\"";

        WireMock.stubFor(WireMock
            .post(url)
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue1)))
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue2)))
            .willReturn(WireMock
                .ok()));

        http("上传多个文件") {
            post url
            multiPartFile "data/降龙十八掌.txt"
            multiPartFile "data/独孤九剑.txt"
        }.then {
            Assertions.assertThat(it.response.status).isEqualTo(200)
        }

        httpWith("上传多个文件") {
            request {
                post url
                multiPartFile "data/降龙十八掌.txt"
                multiPartFile "data/独孤九剑.txt"
            }
            validate {
                statusCode 200
            }
        }
    }

    @Test(description = "Json: 值类型为 String 或 Object")
    public void testJson_StringOrObject() {
        WireMock.stubFor(WireMock
            .post("/json/stringOrObject")
            .withHeader(HttpHeader.CONTENT_TYPE.value(), WireMock.containing("application/json"))
            .withRequestBody(WireMock.equalToJson("""
                {
                  "name": "groot",
                  "age": 18
                }"""))
            .willReturn(WireMock
                .ok()));

        sv("name", "groot")
        sv("age", 18)

        http("值类型为 String") {
            post "/json/stringOrObject"
            json '''
                {
                  "name": "groot",
                  "age": 18
                }
                '''
        }

        http("值类型为 Object") {
            post "/json/stringOrObject"
            json(["name": "groot", "age": 18])
        }

        http("在 String 中使用表达式") {
            post "/json/stringOrObject"
            json('''
                {
                  "name": "${name}",
                  "age": ${age}
                }
                ''')
        }

        http("在 Object 中使用表达式") {
            post "/json/stringOrObject"
            json([
                'name': '${name}',
                'age' : '${age}'
            ])
        }
    }

}
