package com.liyunx.groot.protocol.http

import com.alibaba.fastjson2.JSON
import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.matchers.ProxyMatchers
import com.liyunx.groot.support.Ref
import com.liyunx.groot.testelement.controller.RepeatController
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.lv
import static com.liyunx.groot.DefaultVirtualRunner.repeatWith
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith
import static com.liyunx.groot.support.Ref.ref
import static groovy.lang.Closure.DELEGATE_ONLY
import static org.assertj.core.api.Assertions.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.lessThan
/**
 * Groovy 风格用例（推荐，写法更简洁）
 */
class GroovyTest extends WireMockTestNGTestCase {

    def detailValue = "http-get-test"

    @Test(description = "Teardown 测试")
    void testTeardown() {
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

        Ref<String> detail = ref("")
        httpWith("Get 请求") {
            request {
                get url
            }
            extract {
                jsonpath 'detail', '$.data.detail'
                assert lv('detail') == "http-get-test"

                jsonpath detail, '$.data.detail'
                assert detail.value == "http-get-test"

                String detail1 = jsonpath('$.data.detail')
                assert detail1 == "http-get-test"
            }
            validate {
                body containsString(lv('detail'))
                body containsString(detail.value)
            }
        }

    }

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
                msg = jsonpath('$.message')
            } validate {
                // 方法名冲突时，通过 对象.方法 来区分委托对象的方法和静态导入方法
                def should = delegate

                statusCode 200
                statusCode(
                    ProxyMatchers.equalTo('${180 + 20}'),
                    lessThan(300),
                    ProxyMatchers.equalTo('${20 * 10}'))
                body responseBody
                body containsString(msg)
                assertThat(msg).isEqualTo("OK")

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

    @Test(description = "测试 Repeat 控制器")
    void testRepeat() {
        // 写法一：配置和前后置构建放在 repeatWith 参数位置，适合构建代码比较简单的情况
        Ref<Integer> count = ref(0);
        repeatWith("重复 3 次", {
            times("3")
            variables {
                var("x", "y")
            }
            setupBefore {
                hook('${vars.put("x", "z")}')
            }
            validate {
                equalTo("\${x}", "z")
            }
        }) {
            count.value++;
        }
        assertThat(count.value).isEqualTo(3)

        // 写法二
        repeatWith("重复 3 次") {
            times("3")
            variables {
                var("x", "y")
            }
            setupBefore {
                hook('${vars.put("x", "z")}')
            }
            validate {
                equalTo("\${x}", "z")
            }
            steps {
                count.value++;
            }
        }
        assertThat(count.value).isEqualTo(6)

        Closure cl = repeatClosure {
            times("3")
            variables {
                var("x", "y")
            }
            setupBefore {
                hook('${vars.put("x", "z")}')
            }
            validate {
                equalTo("\${x}", "z")
            }
        }
        repeatWith("重复 3 次", cl) {
            count.value++;
        }
    }

    private static <T> Closure<T> repeatClosure(@DelegatesTo(strategy = DELEGATE_ONLY, value = RepeatController.Builder.class) Closure<T> cl) {
        return cl
    }

}
