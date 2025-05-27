package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;

import java.lang.reflect.Type;
import java.util.List;

/**
 * ConfigItem JSON String to ConfigItem Object
 */
@SuppressWarnings("rawtypes")
public class ConfigItemObjectReader implements ObjectReader<ConfigItem> {

    @Override
    public ConfigItem readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 配置项 key，如 http
        String jsonKey = jsonReader.getFieldName();
        // 配置项类型，如 HttpConfigItem
        Class<? extends ConfigItem> clazz = ApplicationConfig.getConfigItemKeyMap().get(jsonKey);
        if (clazz == null)
            throw new JSONException(String.format("数据加载失败，%s 对应的 ConfigItem 实现类缺失，或不支持 %s 配置",
                jsonKey,
                jsonKey));

        Object value = jsonReader.readAny();
        // 非标准 JSON 转标准 JSON
        // 拦截器链式处理：如果需要转换，则使用指定拦截策略
        Object standardData = null;
        List<FastJson2Interceptor> interceptors = ApplicationConfig.getFastJson2Interceptors();
        for (FastJson2Interceptor interceptor : interceptors) {
            standardData = interceptor.deserializeConfigItem(clazz, value);
            if (standardData != null) {
                break;
            }
        }
        // 如果不需要转换，则使用默认策略
        if (standardData == null) standardData = value;

        return JSON.parseObject(JSON.toJSONString(standardData), clazz);
    }

}
