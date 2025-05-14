package com.liyunx.groot.protocol.http.processor

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.mapping.Mappings
import com.liyunx.groot.mapping.MappingsBuilder
import com.liyunx.groot.mapping.internal.InternalNoArgumentsTestMapping
import com.liyunx.groot.matchers.ProxyMatchers
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import org.hamcrest.Matchers
import org.testng.annotations.Test

import java.util.function.Function

import static com.liyunx.groot.mapping.internal.InternalArgumentsTestMapping.__internal_arguments_test_mapping
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith
import static org.hamcrest.Matchers.equalTo

class MatcherAssertionGroovyTest extends WireMockTestNGTestCase {

    @Test
    public void testSingleMapper() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.okJson("""
                {
                    "name": jack,
                    "age: 18
                }
                """)));

        httpWith("单个 mapper 测试") {
            request {
                get "/get"
            }
            validate {
                header "Content-Type", InternalNoArgumentsTestMapping.__INTERNAL_NO_ARGUMENTS_TEST_MAPPING,
                    equalTo("<<<application/json>>>")
                header "Content-Type", __internal_arguments_test_mapping("(((", ")))"),
                    equalTo("(((application/json)))")
                header "Content-Type", (s -> s + "!!!"),
                    equalTo("application/json!!!")
            }
        }
    }

    @Test
    public void testMultiMapper() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.okJson("""
                {
                    "name": jack,
                    "age: 18
                }
                """)));

        httpWith("多个 mapper 测试") {
            request {
                get "/get"
            }
            validate {
                statusCode(
                    Function.<Integer> identity()
                        .andThen(Mappings.toStr())
                        .andThen(__internal_arguments_test_mapping("(((", ")))"))
                        .andThen(InternalNoArgumentsTestMapping.__INTERNAL_NO_ARGUMENTS_TEST_MAPPING)
                        .andThen(s -> s + "!!!"),
                    equalTo("<<<(((200)))>>>!!!"))
                statusCode(
                    MappingsBuilder.<Integer, String> mappings()
                        .toStr()
                        .map(__internal_arguments_test_mapping("(((", ")))"))
                        .map(InternalNoArgumentsTestMapping.__INTERNAL_NO_ARGUMENTS_TEST_MAPPING)
                        .map(s -> s + "!!!")
                        .build(),
                    equalTo("<<<(((200)))>>>!!!"))
            }
        }
    }

    @Test
    public void testType() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.okJson("""
                {
                    "name": jack,
                    "age: 18
                }
                """)));

        httpWith("多个 mapper 测试") {
            request {
                get "/get"
            }
            validate {
                statusCode Mappings.toStr(),
                    Matchers.hasLength(3),
                    Matchers.equalTo("200"),
                    ProxyMatchers.equalTo("\${199 + 1}"),
                    ProxyMatchers.equalTo(String.class, "\${199 + 1}")
            }
        }
    }
}


