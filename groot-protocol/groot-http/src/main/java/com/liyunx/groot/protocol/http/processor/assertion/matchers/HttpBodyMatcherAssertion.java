package com.liyunx.groot.protocol.http.processor.assertion.matchers;

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
 * HTTP 响应状态码断言
 */
@KeyWord(HttpSampler.KEY + TestElementKeyWord.SEPARATOR + HttpBodyMatcherAssertion.KEY)
@MatcherValueType(String.class)
@SuppressWarnings("rawtypes")
public class HttpBodyMatcherAssertion extends MatcherAssertion<String> {

    private static final Logger log = LoggerFactory.getLogger(HttpBodyMatcherAssertion.class);

    public static final String KEY = "body";

    public HttpBodyMatcherAssertion() {
    }

    private HttpBodyMatcherAssertion(Builder builder) {
        super(builder);
    }

    @Override
    protected String extractInitialValueOfActual(ContextWrapper ctx) {
        log.info("响应体断言{}", name == null ? "" : "，" + name);
        HttpSampleResult result = (HttpSampleResult) ctx.getTestResult();
        return result.getResponse().getBody();
    }

    @Override
    public String name() {
        return name == null ? "响应体断言" : name;
    }

    public static class Builder extends MatcherAssertion.Builder<HttpBodyMatcherAssertion, String, Builder> {

        public HttpBodyMatcherAssertion build() {
            return new HttpBodyMatcherAssertion(this);
        }

    }

}
