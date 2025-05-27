package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.processor.PostProcessor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.constants.TestElementKeyWord.*;

/**
 * PostProcessor JSON String to PostProcessor Object
 */
@SuppressWarnings("unchecked")
public class PostProcessorObjectReader implements ObjectReader<PostProcessor> {

    @Override
    public PostProcessor readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 读取后置处理器原始数据
        Map<String, Object> postProcessorMap = jsonReader.readObject();
        String type = (String) postProcessorMap.get(TEAR_DOWN_TYPE);
        postProcessorMap.remove(TEAR_DOWN_TYPE);
        Map.Entry<String, Object> entry = postProcessorMap.entrySet().stream().findFirst().get();
        String key = entry.getKey();
        Object value = entry.getValue();

        // 读取后置处理器类型
        Class<? extends PostProcessor> clazz;
        switch (type) {
            case EXTRACT:
                clazz = ApplicationConfig.getExtractorKeyMap().get(key);
                if (clazz == null)
                    throw new JSONException(String.format("%s 不是支持的 Extractor 类型", key));
                break;
            case VALIDATE:
                clazz = ApplicationConfig.getAssertionKeyMap().get(key);
                if (clazz == null)
                    throw new JSONException(String.format("%s 不是支持的 Assertion 类型", key));
                break;
            default:
                clazz = ApplicationConfig.getPostProcessorKeyMap().get(key);
                if (clazz == null)
                    throw new JSONException(String.format("%s 不是支持的 PostProcessor 类型", key));
                break;
        }

        // 原始数据转标准 JSON 数据
        Map<String, Object> dataMap = getPostProcessorData(clazz, value);

        // JSON 表示转 PostProcessor 对象
        return JSON.parseObject(JSON.toJSONString(dataMap), clazz);
    }

    public static Map<String, Object> getPostProcessorData(Class<? extends PostProcessor> clazz, Object value) {
        // 非标准 JSON 转标准 JSON
        // 拦截器链式处理：如果需要转换，则使用指定拦截策略
        Map<String, Object> map;
        List<FastJson2Interceptor> interceptors = ApplicationConfig.getFastJson2Interceptors();
        for (FastJson2Interceptor interceptor : interceptors) {
            map = interceptor.deserializePostProcessor(clazz, value);
            if (map != null) {
                return map;
            }
        }
        // 如果不需要转换，则使用默认策略
        if (value instanceof Map) {
            return  (Map<String, Object>) value;
        }
        throw new GrootException(
            "%s 无法反序列化为 %s 对象，当前值: %s ",
            value == null ? "null" : value.getClass().getName(),
            clazz.getName(),
            value == null ? "null" : JSON.toJSONString(value, JSONWriter.Feature.PrettyFormat));
    }

}
