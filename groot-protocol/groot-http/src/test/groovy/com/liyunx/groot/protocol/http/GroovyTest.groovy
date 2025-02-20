package com.liyunx.groot.protocol.http

import com.alibaba.fastjson2.JSON
import com.liyunx.groot.matchers.ProxyMatchers
import com.github.tomakehurst.wiremock.client.WireMock
import com.jayway.jsonpath.JsonPath
import org.testng.annotations.Test

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith
import static org.assertj.core.api.Assertions.assertThat
import static org.hamcrest.Matchers.lessThan
/**
 * Groovy 风格用例（推荐，写法更简洁）
 */
class GroovyTest extends WireMockTestNGTestCase {

    def detailValue = "http-get-test"

    @Test(description = "Get 请求示例")
    void testGet() {
        String url = "/get"
        String responseBody = JSON.toJSONString(
            [
                "code"   : 200,
                "message": "OK",
                "data"   : [
                    "detail": "http-get-test"
                ]
            ])

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson(responseBody)))

        String msg
        httpWith("Get 请求") {
            request {
                get url
            } extract {
                jsonpath 'detail', '$.data.detail'
                applyR {
                    msg = JsonPath.parse(it.response.body).read('$.message')
                }
            } validate {
                // 方法名冲突时，通过 对象.方法 来区分委托对象的方法和静态导入方法
                def should = delegate

                statusCode 200
                statusCode(
                    ProxyMatchers.equalTo('${180 + 20}'),
                    lessThan(300),
                    ProxyMatchers.equalTo('${20 * 10}'))
                body responseBody
                // 这样的写法是错误的，Builder 构建时，msg 并未赋值
                //body containsString(msg)
                apply {
                    assertThat(msg).isEqualTo("OK")
                }

                should.equalTo '${detail}', "http-get-test"
                // 访问类成员变量或方法使用 this.xxx
                should.equalTo '${detail}', this.detailValue
            }
        }
    }

    @Test(description = "除默认 API 外，也可以使用额外的代码辅助构建")
    void testGetWithHelpCode() {
        String url = "/post"

        WireMock.stubFor(WireMock
            .post(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson("")))

        httpWith("辅助构建") {
            request {
                post url
                // 使用代码辅助请求构建
                5.times {
                    header "header$it", "value$it"
                }
                body {
                    '重复 1000 次作为请求体' * 1000
                }
            } validate {
                applyR {
                    def headers = it.request.headers
                    assertThat(headers.size()).isGreaterThan(5)
                    assertThat(headers[0].value).isEqualTo("value0")
                    assertThat(headers[4].value).isEqualTo("value4")
                }
            }
        }
    }

}
