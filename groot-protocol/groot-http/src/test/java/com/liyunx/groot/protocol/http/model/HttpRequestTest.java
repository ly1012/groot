package com.liyunx.groot.protocol.http.model;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.MultipartValuePattern;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class HttpRequestTest extends WireMockTestNGTestCase {

    @Test(description = "URL 测试：路径变量")
    public void testGet_PathVariable() {
        String url = "/get/1/detail/groot";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlEqualTo(url))
            .willReturn(WireMock
                .okJson("{}")));

        httpWith("路径变量测试", action -> action
            .request(it -> it
                .get("/get/:id/detail/:name")
                .pathVariable("id", "1")
                .pathVariable("name", "groot"))
            .validate(it -> it
                .statusCode(200)
                .applyR(r -> {
                    try {
                        URL fullURL = new URL(r.getRequest().getUrl());
                        assertThat(fullURL.getPath(), equalTo(url));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })));
    }

    @Test(description = "URL 测试：查询参数")
    public void testGet_QueryParam() {
        String url = "/get";

        WireMock.stubFor(WireMock
            .get(WireMock
                .urlPathEqualTo(url))
            .withQueryParam("page", WireMock.equalTo("4"))
            .withQueryParam("limit", WireMock.equalTo("20"))
            .withQueryParam("name", WireMock.equalTo("cat"))
            .withQueryParam("name", WireMock.equalTo("dog"))
            .willReturn(WireMock
                .okJson("{}")));

        httpWith("查询参数测试", action -> action
            .request(it -> it
                .get(url)
                .queryParam("page", "4")
                .queryParam("limit", "20")
                .queryParam("name", "cat", "dog"))
            .validate(it -> it
                .statusCode(200)
                .applyR(r -> {
                    QueryParamManager params = r.getRequest().getParams();
                    assertQueryParam(params, 0, "page", "4");
                    assertQueryParam(params, 1, "limit", "20");
                    assertQueryParam(params, 2, "name", "cat");
                    assertQueryParam(params, 3, "name", "dog");
                })));
    }

    private void assertQueryParam(QueryParamManager params, int index, String name, String value) {
        QueryParam param = params.get(index);
        assertThat(param.getName(), equalTo(name));
        assertThat(param.getValue(), equalTo(value));
    }

    @Test(description = "Post Body：multiPartFile")
    void testPost_MultiPartFile() {
        String url = "/multipart/chinese";
        String contentDispositionValue1 = "form-data; name=\"file\"; filename=\"中文.txt\"";
        String contentDispositionValue2 = "form-data; name=\"file\"; filename=\"中文.pdf\"";

        WireMock.stubFor(WireMock
            .post(url)
            // 默认匹配类型为 MultipartValuePattern.MatchingType.ANY
            // ANY：请求体中只要有一个 Part 匹配该 multiPartPattern 即成功
            // ALL：请求体中所有 Part 都匹配该 multiPartPattern 即成功
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue1)))
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue2)))
            .withMultipartRequestBody(WireMock.aMultipart()
                .matchingType(MultipartValuePattern.MatchingType.ALL)
                .withHeader("Content-Disposition", WireMock.containing("中文")))
            .willReturn(WireMock
                .ok()));

        httpWith("post multipart/form-data body", action -> action
            .request(request -> request
                .post(url)
                .multiPartFile("data/中文.txt")
                .multiPartFile("data/中文.pdf"))
            .validate(validate -> validate
                .statusCode(200)
                .applyR(r -> {
                    String body = r.getRequest().getBody();
                    assertThat(body, containsString(contentDispositionValue1));
                    assertThat(body, containsString(contentDispositionValue2));
                })));
    }

}
