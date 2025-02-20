package com.liyunx.groot.protocol.http.processor.extractor;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.constants.TestElementKeyWord;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.model.TestStatus;
import com.liyunx.groot.processor.extractor.AbstractExtractor;
import com.liyunx.groot.processor.extractor.ExtractResult;
import com.liyunx.groot.protocol.http.HttpSampleResult;
import com.liyunx.groot.protocol.http.HttpSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.util.Objects.nonNull;

@KeyWord(HttpSampler.KEY + TestElementKeyWord.SEPARATOR + HttpHeaderExtractor.KEY)
public class HttpHeaderExtractor extends AbstractExtractor<String> {

    private static final Logger log = LoggerFactory.getLogger(HttpHeaderExtractor.class);

    public static final String KEY = "header";

    @JSONField(name = "headerName")
    private String headerName;

    public HttpHeaderExtractor() {}

    private HttpHeaderExtractor(Builder builder) {
        super(builder);
        this.headerName = builder.headerName;
    }

    @Override
    protected ExtractResult extract(ContextWrapper ctx) {
        log.info("响应头提取（多值时提取第一个），{}ref: {}, refName: {}, headerName: {}, defaultValue: {}, scope: {}",
            name == null ? "" : name + "，",
            ref, refName, headerName, defaultValue, scope);

        ExtractResult res = new ExtractResult();
        HttpSampleResult result = (HttpSampleResult) ctx.getTestResult();
        String headerValue = result.getResponse().getHeaders().get(headerName);

        if (nonNull(headerValue)) {
            res.setValue(headerValue);
            return res;
        }

        res.setStatus(TestStatus.FAILED);
        res.setMessage(String.format("响应头中不存在名称为 %s 的 Header", headerName));
        return res;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String name() {
        return name == null ? "响应头提取（"+ headerName + "）"  : name;
    }

    @Override
    public String description() {
        // TODO 简洁完整的描述，null 字段不加入描述
        return super.description();
    }

    public static class Builder extends AbstractExtractor.Builder<HttpHeaderExtractor, String, Builder> {

        private String headerName;

        public Builder headerName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        @Override
        public HttpHeaderExtractor build() {
            return new HttpHeaderExtractor(this);
        }

    }

}
