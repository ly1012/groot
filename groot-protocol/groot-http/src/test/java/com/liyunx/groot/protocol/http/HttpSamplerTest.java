package com.liyunx.groot.protocol.http;

import com.liyunx.groot.mapping.Mappings;
import com.liyunx.groot.matchers.ProxyMatchers;
import com.liyunx.groot.processor.extractor.standard.JsonPathExtractor;
import com.liyunx.groot.support.Ref;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import org.testng.annotations.Test;

import java.util.function.Function;

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;
import static com.liyunx.groot.support.Ref.ref;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class HttpSamplerTest extends WireMockTestNGTestCase {

    @Test(description = "teardown 测试")
    public void testTearDown() {
        String url = "/get";
        String responseBody =
            """
                {
                    "code": 200,
                    "message": "OK",
                    "data": {
                        "id": "root888",
                        "name": "groot"
                    }
                }
                """;

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson(responseBody)));

        httpWith("teardown API", action -> action
            .request(request -> request
                .get(url))
            .teardown(teardown -> teardown
                .applyR(r -> assertThat(r.getResponse().getStatus()).isEqualTo(200))
                .apply(new JsonPathExtractor.Builder().refName("name").expression("$.data.name").build())
                .extract(extract -> extract
                    .header("header1", "Content-Type")
                    .jsonpath("id", "$.data.id"))
                .validate(validate -> validate
                    .equalTo("${header1}", "application/json", true)
                    .equalTo("${name}", "groot"))));
    }

    @Test(description = "extract.header 将提取值赋值给函数外的变量")
    public void testExtract_Header_Ref() {
        String url = "/get";
        String responseBody =
            """
                {
                    "code": 200,
                    "message": "OK",
                    "data": {
                        "detail": "http-ref-test"
                    }
                }
                """;

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson(responseBody)
                .withHeader("testName", "testExtract_Header_Ref")));

        Ref<String> testName = ref();
        httpWith("http-ref-test", action -> action
            .request(it -> it.get(url))
            .extract(it -> it.header(testName, "testName")));
        assertThat(testName.value).isEqualTo("testExtract_Header_Ref");
    }

    @Test(description = "validate.statusCode 断言")
    public void testValidate_StatusCode() {
        String url = "/get";
        String responseBody = "";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson(responseBody)));

        httpWith("statusCode 断言", action -> action
            .request(it -> it
                .get(url))
            .validate(it -> it
                .statusCode(200)
                .statusCode(greaterThan(100), lessThan(300))
                .statusCode(ProxyMatchers.equalTo("${r.response.status}"))
                .statusCode(ProxyMatchers.equalTo("${ 201 - 1 }"))
                .statusCode(
                    Function.<Integer>identity()
                        .andThen(Mappings.toStr())
                        .andThen(Mappings.toInt())
                        .andThen(Mappings.toStr())
                        .andThen(s -> s + "L"),
                    equalTo("200L")))
        );
    }

    @Test(description = "validate.header 断言")
    public void testValidate_Header() {
        String url = "/get";
        String responseBody = "";

        WireMock.stubFor(WireMock
            .get(WireMock.urlEqualTo(url))
            .willReturn(WireMock
                .okJson(responseBody)
                .withHeader("myHeader1", "myValue1")
                .withHeader("myHeader2", "myValue2")
                .withHeader("myHeader3", "879")));

        httpWith("header 断言", action -> action
            .request(it -> it
                .get(url))
            .validate(it -> it
                .statusCode(200)
                .header("myHeader1", "myValue1")
                .header("myHeader2", containsString("myValue"))
                .header("myHeader3", Mappings.toInt(), greaterThan(500)))
        );
    }

    @Test(description = "validate.body 断言")
    public void testValidate_Body() {
        String url = "/get";
        String responseBody =
            """
                {
                    "code": 200,
                    "message": "OK",
                    "data": {
                        "id": "root888",
                        "name": "groot"
                    }
                }
                """;

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson(responseBody)));

        httpWith("body 断言", action -> action
            .request(it -> it
                .get(url))
            .validate(it -> it
                .statusCode(200)
                .body(responseBody)
                .body(matchesRegex("[\\s\\S]*\"id\": \"\\S{4}\\d{3}\"[\\s\\S]*"))
                .body(s -> JsonPath.parse(s).read("$.data.id"), equalTo("root888"))));

        http("body 断言", it -> it
            .get(url)
        ).then(r -> {
            HttpRealResponse response = r.getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(responseBody);
        });
    }

}
