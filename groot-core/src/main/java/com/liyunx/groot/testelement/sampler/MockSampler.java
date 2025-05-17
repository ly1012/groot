package com.liyunx.groot.testelement.sampler;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.builder.*;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.testelement.RealRequest;
import com.liyunx.groot.testelement.RealResponse;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * 模拟一个请求，用于测试或调试场景
 */
@KeyWord(MockSampler.KEY)
public class MockSampler extends AbstractSampler<MockSampler, DefaultSampleResult> {

    public static final String KEY = "mock";

    @JSONField(name = KEY)
    private MockRequest request;

    public MockSampler() {
    }

    private MockSampler(Builder builder) {
        super(builder);
        this.request = builder.request;
    }

    // ---------------------------------------------------------------------
    // 重写 AbstractTestElement 方法
    // ---------------------------------------------------------------------

    @Override
    protected DefaultSampleResult createTestResult() {
        return new DefaultSampleResult();
    }

    // ---------------------------------------------------------------------
    // 重写 AbstractSampler 方法
    // ---------------------------------------------------------------------

    @Override
    protected void sample(ContextWrapper contextWrapper, DefaultSampleResult result) {
        RealRequest realRequest = new RealRequest();
        realRequest.setBody(running.request.requestBody);
        result.setRequest(realRequest);

        RealResponse realResponse = new RealResponse();
        realResponse.setBody(running.request.responseBody);
        result.setResponse(realResponse);
    }

    @Override
    public void recover(SessionRunner session) {
        super.recover(session);
        running.request = request.copy();
    }

    @Override
    public MockSampler copy() {
        MockSampler self = super.copy();
        self.request = request;
        return self;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (DebugSampler)
    // ---------------------------------------------------------------------

    public MockRequest getRequest() {
        return request;
    }

    public void setRequest(MockRequest request) {
        this.request = request;
    }

    public static class MockRequest implements Copyable<MockRequest> {

        private String requestBody;
        private String responseBody;

        public MockRequest() {
        }

        public MockRequest(Builder builder) {
            this.requestBody = builder.reqeustBody;
            this.responseBody = builder.responseBody;
        }

        public String getRequestBody() {
            return requestBody;
        }

        public void setRequestBody(String requestBody) {
            this.requestBody = requestBody;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public MockRequest copy() {
            MockRequest request = new MockRequest();
            request.requestBody = requestBody;
            request.responseBody = responseBody;
            return request;
        }

        public static class Builder {
            private String reqeustBody;
            private String responseBody;

            public Builder requestBody(String requestBody) {
                this.reqeustBody = requestBody;
                return this;
            }

            public Builder responseBody(String responseBody) {
                this.responseBody = responseBody;
                return this;
            }

            public MockRequest build() {
                return new MockRequest(this);
            }
        }

    }

    // ---------------------------------------------------------------------
    // Builder (DebugSampler.Builder)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractSampler.Builder<
        MockSampler, Builder,
        CommonConfigBuilder,
        CommonPreProcessorsBuilder<MockSampler>,
        CommonPostProcessorsBuilder<DefaultSampleResult>, CommonExtractorsBuilder<DefaultSampleResult>, CommonAssertionsBuilder<DefaultSampleResult>> {

        private MockRequest request;

        @Override
        protected CommonConfigBuilder getConfigBuilder() {
            return new CommonConfigBuilder();
        }

        @Override
        protected CommonPreProcessorsBuilder<MockSampler> getSetupBuilder(ContextWrapper ctx) {
            return new CommonPreProcessorsBuilder<>(ctx);
        }

        @Override
        protected CommonPostProcessorsBuilder<DefaultSampleResult> getTeardownBuilder(ContextWrapper ctx) {
            return new CommonPostProcessorsBuilder<>(this, ctx);
        }

        @Override
        protected CommonExtractorsBuilder<DefaultSampleResult> getExtractBuilder(ContextWrapper ctx) {
            return new CommonExtractorsBuilder<>(ctx);
        }

        @Override
        protected CommonAssertionsBuilder<DefaultSampleResult> getAssertBuilder(ContextWrapper ctx) {
            return new CommonAssertionsBuilder<>(ctx);
        }

        public Builder request(Customizer<MockRequest.Builder> mockRequest) {
            MockRequest.Builder mockRequestBuilder = new MockRequest.Builder();
            mockRequest.customize(mockRequestBuilder);
            this.request = mockRequestBuilder.build();
            return self;
        }

        public Builder request(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = MockRequest.Builder.class) Closure<?> cl) {
            MockRequest.Builder mockRequestBuilder = new MockRequest.Builder();
            GroovySupport.call(cl, mockRequestBuilder);
            this.request = mockRequestBuilder.build();
            return self;
        }

        @Override
        public MockSampler build() {
            return new MockSampler(this);
        }
    }

}
