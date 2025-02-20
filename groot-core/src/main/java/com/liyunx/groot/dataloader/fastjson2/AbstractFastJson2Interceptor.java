package com.liyunx.groot.dataloader.fastjson2;

import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.processor.PostProcessor;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.testelement.TestElement;
import org.hamcrest.Matcher;

import java.util.Map;

/**
 * 抽象类，方便重写需要的方法
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractFastJson2Interceptor implements FastJson2Interceptor {

    @Override
    public <T extends ConfigItem<?>> Map<String, Object> deserializeConfigItem(Class<T> clazz, Object value) {
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
    public Matcher deserializeMatcher(Class clazz, String type, String matcherKey, Object matcherValue) {
        return null;
    }

    @Override
    public String serialize() {
        return null;
    }

}
