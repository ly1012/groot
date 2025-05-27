package com.liyunx.groot.dataloader.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.processor.PostProcessor;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.testelement.TestElement;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

import static com.liyunx.groot.dataloader.fastjson2.deserializer.MatcherObjectReader.TYPE_KEY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * 抽象类，方便重写需要的方法
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractFastJson2Interceptor implements FastJson2Interceptor {

    @Override
    public <T extends ConfigItem<?>> Object deserializeConfigItem(Class<T> clazz, Object value) {
        return null;
    }

    @Override
    public <T extends TestElement<?>> Map<String, Object> deserializeTestElement(Class<T> clazz, Map<String, Object> value) {
        return null;
    }

    @Override
    public <T extends PreProcessor> Map<String, Object> deserializePreProcessor(Class<T> clazz, Object value) {
        return null;
    }

    @Override
    public <T extends PostProcessor> Map<String, Object> deserializePostProcessor(Class<T> clazz, Object value) {
        return null;
    }

    @Override
    public Matcher deserializeMatcher(List<Class> clazz, List<String> type, String matcherKey, Object matcherValue) {
        return null;
    }

    protected static Class getFirst(List<Class> clazz) {
        return isNull(clazz) ? null : clazz.get(0);
    }

    protected static Iterable subMatchers(List<Class> clazz, List<String> type, Object matcherValue) {
        List subMatchersJsonData = (List) matcherValue;

        // 值类型传递
        if (nonNull(clazz)) {
            for (Object matcherJsonData : subMatchersJsonData) {
                if (matcherJsonData instanceof Map) {
                    ((Map) matcherJsonData).putIfAbsent(TYPE_KEY, type);
                }
            }
        }

        // 计算子 Matcher
        return JSON.parseArray(JSON.toJSONString(subMatchersJsonData), Matcher.class);
    }

}
