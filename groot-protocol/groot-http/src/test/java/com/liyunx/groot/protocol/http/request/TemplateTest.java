package com.liyunx.groot.protocol.http.request;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;

public class TemplateTest extends WireMockTestNGTestCase {

    @Test(description = "引用接口 API")
    public void testRefApi() {
        WireMock.stubFor(WireMock
            .post("/coupon/templet")
            .withRequestBody(WireMock.equalToJson("""
                {
                  "templetType": "0",
                  "discountValue": 33,
                  "maxDiscountAmt": 111,
                  "validDays": 60,
                  "validType": "0",
                  "usablePlatform": {
                    "wx": "1",
                    "app": "1",
                    "pc": "1"
                  }
                }
                """))
            .willReturn(WireMock.ok()));

        getSession().run("testcases/template/refApi.yml");
    }

    @Test(description = "引用接口请求模板")
    public void testRefTemplate() {
        WireMock.stubFor(WireMock
            .post("/coupon/templet")
            .withRequestBody(WireMock.and(
                WireMock.matchingJsonPath("$.discountValue", WireMock.equalTo("33")),
                WireMock.matchingJsonPath("$.maxDiscountAmt", WireMock.equalTo("100"))))
            .willReturn(WireMock.ok()));

        getSession().run("testcases/template/refTemplate.yml");
    }

    @Test(description = "引用测试用例")
    public void testRefTestCase() {
        WireMock.stubFor(WireMock
            .post("/coupon/templet")
            .willReturn(WireMock.ok()));

        getSession().run("testcases/template/refTestCase.yml");
    }

}
