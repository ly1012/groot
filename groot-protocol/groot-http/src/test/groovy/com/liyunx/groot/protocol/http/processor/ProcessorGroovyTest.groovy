package com.liyunx.groot.protocol.http.processor

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.processor.extractor.ExtractScope
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith

class ProcessorGroovyTest extends WireMockTestNGTestCase {


    @Test
    public void testHttpHeaderExtractor() {
        String url = "/get";
        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock.okJson("{}")
                .withHeader("multiHeader", "value1", "value2")));

        httpWith("提取 Header 值") {
            request {
                get url
            }
            extract {
                header "type", "Content-Type", { scope ExtractScope.SESSION }
                header "non", "nonHeader", { defaultValue "noValue" }
            }
            validate {
                equalTo('${type}', "application/json")
                equalTo('${non}', "noValue")
            }
        }
    }

}
