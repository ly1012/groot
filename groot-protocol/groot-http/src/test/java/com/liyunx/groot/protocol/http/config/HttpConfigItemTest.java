package com.liyunx.groot.protocol.http.config;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.liyunx.groot.support.Ref;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpConfigItemTest extends WireMockTestNGTestCase {

    @Test(description = "测试 proxy 配置")
    public void testProxy() {
        String url = "/get";
        Ref<String> realUrl = Ref.ref();
        Ref<String> realHeader = Ref.ref();

        // 启动代理服务器
        WireMockServer proxyServer = new WireMockServer(options()
            // enableBrowserProxying(true) JDK17 报错信息：
            // com.github.tomakehurst.wiremock.http.ssl.CertificateGenerationUnsupportedException:
            // Your runtime does not support generating certificates at runtime
            // 解决方法：启动时增加 --add-exports=java.base/sun.security.x509=ALL-UNNAMED
            // see https://community.wiremock.io/t/16353358/hello-i-am-trying-to-upgrade-my-java-version-from-11-to-17-c
            .enableBrowserProxying(true)
            .dynamicPort()
        );
        proxyServer.addMockServiceRequestListener((request, response) -> {
            // 获取代理服务器收到的请求信息
            realUrl.value = request.getUrl();
            realHeader.value = request.getHeader("testProxyHeader");
        });
        proxyServer.start();
        int proxyPort = proxyServer.port();

        // 添加一个 Mock 接口
        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson("")));

        // 发起请求
        httpWith("test proxy config", action -> action
            .config(config -> config
                .http(http -> http
                    .anyService(any -> any
                        .proxy("127.0.0.1", proxyPort))))
            .request(request -> request
                .get("/get")
                .header("testProxyHeader", "testProxyValue"))
            .validate(validate -> validate
                .apply(it -> {
                    assertThat(realUrl.value).isEqualTo("/get");
                    assertThat(realHeader.value).isEqualTo("testProxyValue");
                })));

        proxyServer.stop();
    }

    @Test(description = "测试 baseUrl 配置")
    public void testBaseUrl() {
        String url = "/get/";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson("")));

        httpWith("test base url config", action -> action
            .config(config -> config
                .http(http -> http
                    .anyService(any -> any
                        .baseUrl(baseUrlWithHttpAndLocalHost() + url))))
            .request(request -> request
                .get(""))
            .validate(validate -> validate
                .statusCode(200)));
    }

    @Test(description = "测试 verify 配置，跳过 SSL 证书校验")
    public void testVerify() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson("")));

        httpWith("test base url config", action -> action
            .config(config -> config
                .http(http -> http
                    .anyService(any -> any
                        .baseUrl(baseUrlWithHttpsAndLocalHost())
                        // 自签名证书，跳过校验
                        .verify(false))))
            .request(request -> request
                .get(url))
            .validate(validate -> validate
                .statusCode(200)));
    }

}
