package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.filter.TestFilter;

import java.lang.reflect.Type;
import java.util.Map;

public class TestFilterObjectReader implements ObjectReader<TestFilter> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public TestFilter readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        Object anyData = jsonReader.readAny();
        if (anyData instanceof String jsonKey) {
            return JSON.parseObject("{}", getClazz(jsonKey));
        }
        if (anyData instanceof Map jsonMap) {
            assert jsonMap.keySet().size() == 1
                : "数据加载失败，数据格式非法，Map 应有且仅有一个 KEY，当前 Map 数据：" + JSON.toJSONString(jsonMap);
            Map.Entry<String, Object> data = ((Map<String, Object>)jsonMap).entrySet().iterator().next();
            String key = data.getKey();
            Object value = data.getValue();
            return JSON.parseObject(JSON.toJSONString(value), getClazz(key));
        }
        throw new JSONException("数据加载失败，数据格式非法（仅支持 String/Map 类型），当前数据：" + JSON.toJSONString(anyData));
    }

    private Class<? extends TestFilter> getClazz(String jsonKey) {
        Class<? extends TestFilter> clazz = ApplicationConfig.getTestFilterKeyMap().get(jsonKey);
        if (clazz == null)
            throw new JSONException(String.format("数据加载失败，%s 对应的 TestFilter 实现类缺失，或不支持 %s 配置",
                jsonKey,
                jsonKey));
        return clazz;
    }

}
