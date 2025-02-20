package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.filter.TestFilter;

import java.lang.reflect.Type;

public class TestFilterObjectReader implements ObjectReader<TestFilter> {

    @Override
    public TestFilter readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 配置项 key，如 userLogin
        String jsonKey = jsonReader.getFieldName();
        // 配置项类型，如 UserLogin
        Class<? extends TestFilter> clazz = ApplicationConfig.getTestFilterKeyMap().get(jsonKey);
        if (clazz == null)
            throw new JSONException(String.format("数据加载失败，%s 对应的 TestElementListener 实现类缺失，或不支持 %s 配置",
                jsonKey,
                jsonKey));
        return jsonReader.read(clazz);
    }

}
