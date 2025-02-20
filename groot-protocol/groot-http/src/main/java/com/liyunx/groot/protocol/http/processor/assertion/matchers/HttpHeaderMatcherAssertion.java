package com.liyunx.groot.protocol.http.processor.assertion.matchers;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.annotation.MatcherValueType;
import com.liyunx.groot.constants.TestElementKeyWord;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.processor.assertion.matchers.MatcherAssertion;
import com.liyunx.groot.protocol.http.HttpSampleResult;
import com.liyunx.groot.protocol.http.HttpSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP 响应头断言
 */
@KeyWord(HttpSampler.KEY + TestElementKeyWord.SEPARATOR + HttpHeaderMatcherAssertion.KEY)
@MatcherValueType(String.class)
@SuppressWarnings("rawtypes")
public class HttpHeaderMatcherAssertion extends MatcherAssertion<String> {

    private static final Logger log = LoggerFactory.getLogger(HttpHeaderMatcherAssertion.class);

    public static final String KEY = "header";

    @JSONField(name = "name")
    private String headerName;

    public HttpHeaderMatcherAssertion() {
    }

    private HttpHeaderMatcherAssertion(Builder builder) {
        super(builder);
        this.headerName = builder.headerName;
    }

    @Override
    protected String extractInitialValueOfActual(ContextWrapper ctx) {
        log.info("响应头断言，{}headerName: {}", name == null ? "" : name + "，", headerName);
        HttpSampleResult result = (HttpSampleResult) ctx.getTestResult();
        return result.getResponse().getHeaders().get(headerName);
    }

    @Override
    public String name() {
        return name == null ? "响应头断言（" + headerName + "）" : name;
    }

    public static class Builder extends MatcherAssertion.Builder<HttpHeaderMatcherAssertion, String, Builder> {

        private String headerName;

        public Builder headerName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        @Override
        public HttpHeaderMatcherAssertion build() {
            return new HttpHeaderMatcherAssertion(this);
        }

    }

}
