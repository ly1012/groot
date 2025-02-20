package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.processor.extractor.Extractor;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Extractor JSON String to Extractor Object
 */
public class ExtractorObjectReader implements ObjectReader<Extractor> {

    @Override
    public Extractor readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 读取 Extractor 原始数据
        Map<String, Object> extractMap = jsonReader.readObject();
        Map.Entry<String, Object> entry = extractMap.entrySet().stream().findFirst().get();
        String key = entry.getKey();
        Object value = entry.getValue();

        // 读取 Extractor 类型
        Map<String, Class<? extends Extractor>> keyMap = ApplicationConfig.getExtractorKeyMap();
        Class<? extends Extractor> clazz = keyMap.get(key);
        // 默认使用 JsonPathExtractor？根据 response type 使用对应 Extractor？
        if (clazz == null) throw new JSONException(String.format("%s 不是支持的提取类型", key));

        // 原始数据转标准 JSON 数据
        Map<String, Object> dataMap = PostProcessorObjectReader.getPostProcessorData(clazz, value);

        // Extractor JSON 表示转 Extractor 对象
        return JSON.parseObject(JSON.toJSONString(dataMap), clazz);
    }

}
