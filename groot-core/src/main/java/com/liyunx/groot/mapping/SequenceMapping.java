package com.liyunx.groot.mapping;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

import static com.liyunx.groot.processor.assertion.matchers.MatcherAssertion.MAPPER_KEY;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SequenceMapping implements MappingFunction<Object, Object> {

    @JSONField(name = MAPPER_KEY)
    private List<MappingFunction> mapper;

    @Override
    public Object apply(Object input) {
        if (mapper == null || mapper.isEmpty()) {
            return input;
        }

        Object r = input;
        for (MappingFunction mapping : mapper) {
            r = mapping.apply(r);
        }
        return r;
    }

    public List<MappingFunction> getMapper() {
        return mapper;
    }

    public void setMapper(List<MappingFunction> mapper) {
        this.mapper = mapper;
    }

}
