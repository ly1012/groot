package com.liyunx.groot.processor.extractor.standard;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.model.TestStatus;
import com.liyunx.groot.processor.extractor.ExtractResult;
import com.liyunx.groot.testelement.TestResult;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JsonPath 提取元件
 */
@KeyWord(JsonPathExtractor.KEY)
public class JsonPathExtractor extends StandardExtractor<Object> {

    public static final String KEY = "jsonpath";

    private static final Logger log = LoggerFactory.getLogger(JsonPathExtractor.class);

    // 目标 JSON 字符串，默认为 r.response.body
    private String target;

    public JsonPathExtractor() {
    }

    private JsonPathExtractor(Builder builder) {
        super(builder);
        this.target = builder.target;
    }

    @Override
    protected ExtractResult extract(ContextWrapper ctx) {
        log.info("JsonPath 提取，{}ref: {}, refName: {}, expression: {}, defaultValue: {}, scope: {}, target: {}",
            name == null ? "" : name + "，",
            ref, refName, expression, defaultValue, scope, target);

        ExtractResult res = new ExtractResult();
        String targetJson = readTargetJSON(ctx, ctx.getTestResult());
        if (targetJson == null || targetJson.trim().isEmpty()) {
            res.setStatus(TestStatus.BROKEN);
            res.setMessage("JSON 字符串为 null 或空，来源：" + (target == null ? "${r.response.body}" : target));
            return res;
        }

        try {
            // compile and parse each time
            Object jsonPathResult = JsonPath.read(targetJson, expression);
            res.setValue(jsonPathResult);
        } catch (PathNotFoundException e) {
            res.setStatus(TestStatus.FAILED);
            res.setMessage(String.format("JsonPath %s 不存在于目标字符串中，目标字符串：\n%s",
                expression, targetJson));
            res.setException(e);
        }

        return res;
    }

    private String readTargetJSON(ContextWrapper ctx, TestResult r) {
        if (target == null) {
            return r.getResponse().getBody();
        }

        Object value = ctx.eval(target);
        if (value == null || value instanceof String) {
            return (String) value;
        }

        return JSON.toJSONString(value);
    }

    @Override
    public String name() {
        return name == null ? "JsonPath 提取（" + expression + "）" : name;
    }

    @Override
    public String description() {
        // TODO 简洁完整的描述，null 字段不加入描述
        return super.description();
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public static class Builder extends StandardExtractor.Builder<JsonPathExtractor, Object, Builder> {

        private String target;

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        @Override
        public JsonPathExtractor build() {
            return new JsonPathExtractor(this);
        }

    }

}


