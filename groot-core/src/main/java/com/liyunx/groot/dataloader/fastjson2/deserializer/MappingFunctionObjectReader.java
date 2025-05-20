package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.mapping.MappingFunction;
import com.liyunx.groot.mapping.SequenceMapping;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.processor.assertion.matchers.MatcherAssertion.MAPPER_KEY;

@SuppressWarnings("rawtypes")
public class MappingFunctionObjectReader implements ObjectReader<MappingFunction> {

    @SuppressWarnings("unchecked")
    @Override
    public MappingFunction readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        Object mappingJsonData = jsonReader.readAny();

        // 无自定义字段的 MappingFunction
        if (mappingJsonData instanceof String) {
            String keyword = (String) mappingJsonData;
            Class<? extends MappingFunction> mappingClass = ApplicationConfig.getMappingKeyMap().get(keyword);
            if (mappingClass == null) {
                throw new GrootException("未找到 %s 关键字对应的 MappingFunction 类", keyword);
            }
            // MappingFunction 没有自定义字段，使用空 JSON 加载
            return JSON.parseObject("{}", mappingClass);
        }

        // 有自定义字段的 MappingFunction
        if (mappingJsonData instanceof Map) {
            Map<String, Object> _mapping = (Map<String, Object>) mappingJsonData;
            Map.Entry<String, Object> mapping = _mapping.entrySet().iterator().next();
            String keyword = mapping.getKey();
            Object value = mapping.getValue();
            Class<? extends MappingFunction> mappingClass = ApplicationConfig.getMappingKeyMap().get(keyword);
            if (mappingClass == null) {
                throw new GrootException("未找到 %s 关键字对应的 MappingFunction 类", keyword);
            }
            return JSON.parseObject(JSON.toJSONString(value), mappingClass);
        }

        if (mappingJsonData instanceof List data) {
            HashMap<String, List> hashMap = new HashMap<>();
            hashMap.put(MAPPER_KEY, data);
            return JSON.parseObject(JSON.toJSONString(hashMap), SequenceMapping.class);
        }

        throw new GrootException("用例格式非法，mapper 列表项的值仅支持 String/Map/List 类型");
    }

}
