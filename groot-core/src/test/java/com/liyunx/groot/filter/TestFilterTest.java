package com.liyunx.groot.filter;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.testelement.sampler.MockSampler;
import com.liyunx.groot.testelement.sampler.SampleResult;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.liyunx.groot.DefaultVirtualRunner.mockWith;
import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;

public class TestFilterTest extends GrootTestNGTestCase {

    @Test
    public void testSamplerAOP() {
        // 创建一个加解密插件
        TestFilter filter = new TestFilter() {

            @Override
            public boolean match(ContextWrapper ctx) {
                return ctx.getTestElement() instanceof MockSampler;
            }

            @Override
            public void doSample(ContextWrapper ctx, SampleFilterChain chain) {
                // 获取运行时数据
                MockSampler mockSampler = ((MockSampler)ctx.getTestElement()).getRunning();
                // 请求加密
                String originRequestBody = mockSampler.getRequest().getRequestBody();
                String encodedRequestBody = Base64.getEncoder().encodeToString(originRequestBody.getBytes(StandardCharsets.UTF_8));
                mockSampler.getRequest().setRequestBody(encodedRequestBody);
                // 发起请求
                chain.doSample(ctx);
                // 响应解密
                SampleResult<?> result = (SampleResult<?>) ctx.getTestResult();
                String originResponseBody = result.getResponse().getBody();
                String decodedResponseBody = new String(Base64.getDecoder().decode(originResponseBody), StandardCharsets.UTF_8);
                result.getResponse().setBody(decodedResponseBody);
            }
        };

        // 针对本用例的所有 MockSampler 请求应用加解密插件
        sessionConfig(config -> config.filters(filter));

        mockWith("加解密测试：请求一", action -> action
            .request(request -> request
                .requestBody("request data")             // 明文请求数据（用户请求数据，会自动加密后发送）
                .responseBody("cmVzcG9uc2UgZGF0YQ=="))   // 密文响应数据（模拟服务端返回的数据）
            .validate(validate -> validate
                .equalTo("${r.request.body}", "cmVxdWVzdCBkYXRh")  // 客户端发送给服务端的数据，密文数据
                .equalTo("${r.response.body}", "response data"))); // 响应数据（自动解密为明文）

        mockWith("加解密测试：请求二", action -> action
            .request(request -> request
                .requestBody("请求数据")
                .responseBody("5ZON5bqU5pWw5o2u"))
            .validate(validate -> validate
                .equalTo("${r.request.body}", "6K+35rGC5pWw5o2u")
                .equalTo("${r.response.body}", "响应数据")));
    }

}
