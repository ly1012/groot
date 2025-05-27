package com.liyunx.groot.protocol.http.processor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.liyunx.groot.processor.extractor.ExtractScope;
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;

public class ProcessorTest extends WireMockTestNGTestCase {

    @Test
    public void testSpecialAccessObjects() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url)));

        httpWith("Get 请求", http -> http
            .request(request -> request
                .get(url)
                .header("token", "gua gua"))
            .setupAfter(setup -> {
                setup.e.getRequest().getHeaders().setHeader("traceId", "traceId123");
            })
            .teardown(teardown -> {
                assert teardown.r.getRequest().getHeaders().getHeader("traceId").getValue() == "traceId123";
                teardown.extract(extract -> {
                    assert extract.r.getRequest().getHeaders().getHeader("token").getValue() == "gua gua";
                }).validate(validate -> {
                    assert validate.r.getRequest().getMethod() == "GET";
                });
            }));
    }

    @Test
    public void testHttpHeaderExtractor() {
        String url = "/get";
        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.okJson("{}")
                .withHeader("multiHeader", "value1", "value2")));

        getSession().run("testcases/processor/extract_header.yml");

        httpWith("提取 Header 值", action -> action
            .request(request -> request
                .get(url))
            .extract(extract -> extract
                .header("type", "Content-Type", params -> params.scope(ExtractScope.SESSION))
                .header("non", "nonHeader", params -> params.defaultValue("noValue")))
            .validate(validate -> validate
                .equalTo("${type}", "application/json")
                .equalTo("${non}", "noValue")));
    }

}
