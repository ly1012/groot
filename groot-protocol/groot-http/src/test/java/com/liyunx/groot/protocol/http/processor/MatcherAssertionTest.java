package com.liyunx.groot.protocol.http.processor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.mapping.MappingFunction;
import com.liyunx.groot.mapping.Mappings;
import com.liyunx.groot.mapping.MappingsBuilder;
import com.liyunx.groot.mapping.internal.InternalArgumentsTestMapping;
import com.liyunx.groot.mapping.internal.InternalNoArgumentsTestMapping;
import com.liyunx.groot.matchers.ProxyMatchers;
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.mapping.internal.InternalArgumentsTestMapping.__internal_arguments_test_mapping;
import static com.liyunx.groot.mapping.internal.InternalNoArgumentsTestMapping.__INTERNAL_NO_ARGUMENTS_TEST_MAPPING;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;
import static org.hamcrest.Matchers.equalTo;

public class MatcherAssertionTest extends WireMockTestNGTestCase {

    @BeforeClass
    public void beforeClass() {
        Map<String, Class<? extends MappingFunction>> mappingKeyMap = new HashMap<>(ApplicationConfig.getMappingKeyMap());
        mappingKeyMap.put("__internal_arguments_test__", InternalArgumentsTestMapping.class);
        mappingKeyMap.put("__internal_no_arguments_test__", InternalNoArgumentsTestMapping.class);
        ApplicationConfig.setMappingKeyMap(mappingKeyMap);
    }

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

        getSession().run("testcases/processor/matcher_single_mapper.yml");
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

        getSession().run("testcases/processor/matcher_multi_mapper.yml");
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

        getSession().run("testcases/processor/matcher_type.yml");
    }

    @Test
    public void testMatchers() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.ok("海内存知己，天涯若比邻。")));

        getSession().run("testcases/processor/matcher_matchers.yml");
    }

    @Test
    public void testSingleMapperByJava() {
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

        httpWith("单个 mapper 测试", action -> action
            .request(request -> request
                .get("/get"))
            .validate(validate -> validate
                .header("Content-Type", __INTERNAL_NO_ARGUMENTS_TEST_MAPPING,
                    equalTo("<<<application/json>>>"))
                .header("Content-Type", __internal_arguments_test_mapping("(((", ")))"),
                    equalTo("(((application/json)))"))
                .header("Content-Type", s -> s + "!!!",
                    equalTo("application/json!!!"))));
    }

    @Test
    public void testMultiMapperByJava() {
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

        httpWith("多个 mapper 测试", action -> action
            .request(request -> request
                .get("/get"))
            .validate(validate -> validate
                .statusCode(
                    Function.<Integer>identity()
                        .andThen(Mappings.toStr())
                        .andThen(__internal_arguments_test_mapping("(((", ")))"))
                        .andThen(__INTERNAL_NO_ARGUMENTS_TEST_MAPPING)
                        .andThen(s -> s + "!!!"),
                    equalTo("<<<(((200)))>>>!!!"))
                .statusCode(
                    MappingsBuilder.<Integer, String>mappings()
                        .toStr()
                        .map(__internal_arguments_test_mapping("(((", ")))"))
                        .map(__INTERNAL_NO_ARGUMENTS_TEST_MAPPING)
                        .map(s -> s + "!!!")
                        .build(),
                    equalTo("<<<(((200)))>>>!!!"))));
    }

    @Test
    public void testTypeByJava() {
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

        httpWith("type 用法", action -> action
            .request(request -> request
                .get("/get"))
            .validate(validate -> validate
                .statusCode(Mappings.toStr(),
                    Matchers.hasLength(3),
                    Matchers.equalTo("200"),
                    ProxyMatchers.equalTo("${199 + 1}"),
                    ProxyMatchers.equalTo(String.class, "${199 + 1}"))));
    }

    @Test
    public void testMatchersByJava() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.ok("海内存知己，天涯若比邻。")));

        httpWith("type 用法", action -> action
            .variables(variables -> variables
                .var("bodyContent", "海内存知己，天涯若比邻。"))
            .request(request -> request
                .get("/get"))
            .validate(validate -> validate
                // 默认使用 equalTo 匹配
                .statusCode(200)
                // 多个 Matcher
                .statusCode(Matchers.lessThan(300), Matchers.greaterThanOrEqualTo(200))
                // allOf 全部匹配
                .statusCode(Matchers.allOf(
                    Matchers.lessThan(300),
                    Matchers.greaterThanOrEqualTo(200)))
                // anyOf 任一匹配
                .statusCode(Matchers.anyOf(
                    Matchers.equalTo(300),
                    Matchers.equalTo(200)))
                .body("海内存知己，天涯若比邻。")
                .body(Matchers.equalTo("海内存知己，天涯若比邻。"), Matchers.containsString("天涯"))
                // 嵌套 Matcher
                .body(Matchers.allOf(
                    Matchers.containsString("天涯"),
                    Matchers.anyOf(
                        Matchers.containsString("海"),
                        Matchers.containsString("江"))))
                // 如果 Matcher 或嵌套 Matcher 中包含表达式，则最外面的 Matcher 必须是 ProxyMatcher
                .body(ProxyMatchers.equalTo("${bodyContent}"))
                .body(ProxyMatchers.anyOf(
                    ProxyMatchers.containsString("${'天' + '涯'}"),
                    Matchers.containsString("江")))));
    }

    @Test
    public void testProxyMatchers() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.ok("海内存知己，天涯若比邻。")));

        httpWith("ProxyMatchers 测试", action -> action
            .variables(variables -> variables
                .var("bodyContent", "海内存知己，天涯若比邻。"))
            .request(request -> request
                .get("/get"))
            .validate(validate -> validate
                // 多个 Matcher
                .statusCode(
                    ProxyMatchers.greaterThan("${-199}"),
                    ProxyMatchers.greaterThan("${199}"))
            ));
    }

}
