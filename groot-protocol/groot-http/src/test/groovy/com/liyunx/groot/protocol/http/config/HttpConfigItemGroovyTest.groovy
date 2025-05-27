package com.liyunx.groot.protocol.http.config

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.sv
import static com.liyunx.groot.SessionRunner.getSession

class HttpConfigItemGroovyTest extends WireMockTestNGTestCase {

    @Test
    void testConfigExample() {
        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo("/get"))
            .willReturn(WireMock
                .okJson("")))

        sv("httpPort", httpPort)
        getSession().run("testcases/config/config_url_headers.yml")
    }

}
