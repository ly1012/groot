package com.liyunx.groot.protocol.http


import com.github.tomakehurst.wiremock.client.WireMock
import org.testng.annotations.Test

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http
import static org.assertj.core.api.Assertions.assertThat

class HttpVirtualRunnerGroovyTest extends WireMockTestNGTestCase {

    @Test
    public void testHttp() {
        WireMock.stubFor(WireMock
            .post("/post/GROOT")
            .willReturn(WireMock
                .ok("T O O R G")))

        http('http test') {
            post '/post/:groot'
            pathVariable 'groot', 'GROOT'
            body 'G R O O T'
        }.then {
            assertThat(it.response.body).isEqualTo("T O O R G")
        }
    }

}
