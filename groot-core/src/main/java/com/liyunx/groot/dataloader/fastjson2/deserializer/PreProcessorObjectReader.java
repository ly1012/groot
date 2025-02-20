package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;
import com.liyunx.groot.processor.PreProcessor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * PreProcessor JSON String to PreProcessor Object
 */
public class PreProcessorObjectReader implements ObjectReader<PreProcessor> {

    @Override
    public PreProcessor readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 读取前置处理器数据
        Map<String, Object> preProcessorMap = jsonReader.readObject();
        Map.Entry<String, Object> entry = preProcessorMap.entrySet().stream().findFirst().get();
        String key = entry.getKey();
        Object value = entry.getValue();

        // 读取前置处理器类型
        Class<? extends PreProcessor> clazz = ApplicationConfig.getPreProcessorKeyMap().get(key);
        if (clazz == null)
            throw new JSONException(String.format("%s 不是支持的 PreProcessor 类型", key));

        // 非标准 JSON 转标准 JSON
        // 拦截器链式处理：如果需要转换，则使用指定拦截策略
        Map<String, Object> map = null;
        List<FastJson2Interceptor> interceptors = ApplicationConfig.getFastJson2Interceptors();
        for (FastJson2Interceptor interceptor : interceptors) {
            map = interceptor.deserializePreProcessor(clazz, value);
            if (map != null) break;
        }
        // 如果不需要转换，则使用默认策略
        if (map == null) map = (Map<String, Object>) value;

        // JSONObject 转 PreProcessor 对象
        return JSON.parseObject(JSON.toJSONString(map), clazz);
    }

}
