package com.liyunx.groot.protocol.http.processor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.mapping.internal.InternalArgumentsTestMapping.__internal_arguments_test_mapping;
import static com.liyunx.groot.mapping.internal.InternalNoArgumentsTestMapping.__INTERNAL_NO_ARGUMENTS_TEST_MAPPING;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;
import static org.hamcrest.Matchers.equalTo;

public class HttpHeaderMatcherAssertionTest extends WireMockTestNGTestCase {

    @Test
    public void testHeaderAssertion() {
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

        getSession().run("testcases/processor/assertion_header.yml");

        httpWith("header 断言", action -> action
            .request(request -> request
                .get("/get"))
            .validate(validate -> validate
                .header("Content-Type", "application/json")
                .header("Content-Type", equalTo("application/json"))
                .header("Content-Type", __INTERNAL_NO_ARGUMENTS_TEST_MAPPING,
                    equalTo("<<<application/json>>>"))
                .header("Content-Type", __internal_arguments_test_mapping("(((", ")))"),
                    equalTo("(((application/json)))"))
                .header("Content-Type", s -> s + "!!!",
                    equalTo("application/json!!!"))));
    }

}
