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
@KeyWord(HttpSampler.KEY + TestElementKeyWord.SEPARATOR + HttpStatusCodeMatcherAssertion.KEY)
@MatcherValueType(Integer.class)
@SuppressWarnings({"rawtypes"})
public class HttpStatusCodeMatcherAssertion extends MatcherAssertion<Integer> {

    private static final Logger log = LoggerFactory.getLogger(HttpStatusCodeMatcherAssertion.class);

    public static final String KEY = "statusCode";

    public HttpStatusCodeMatcherAssertion() {
    }

    private HttpStatusCodeMatcherAssertion(Builder builder) {
        super(builder);
    }

    @Override
    protected Integer extractInitialValueOfActual(ContextWrapper ctx) {
        log.info("状态码断言{}", name == null ? "" : "，" + name);
        HttpSampleResult result = (HttpSampleResult) ctx.getTestResult();
        return result.getResponse().getStatus();
    }

    @Override
    public String name() {
        return name == null ? "状态码断言" : name;
    }

    public static class Builder extends MatcherAssertion.Builder<HttpStatusCodeMatcherAssertion, Integer, Builder> {

        @Override
        public HttpStatusCodeMatcherAssertion build() {
            return new HttpStatusCodeMatcherAssertion(this);
        }

    }

}


