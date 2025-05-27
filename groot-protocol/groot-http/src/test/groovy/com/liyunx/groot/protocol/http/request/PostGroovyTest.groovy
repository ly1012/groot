package com.liyunx.groot.protocol.http.request

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import com.liyunx.groot.protocol.http.constants.HttpHeader
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.sv
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http

class PostGroovyTest extends WireMockTestNGTestCase {


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
