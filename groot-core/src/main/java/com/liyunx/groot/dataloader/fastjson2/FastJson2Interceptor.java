package com.liyunx.groot.dataloader.fastjson2;

import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.processor.PostProcessor;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.testelement.TestElement;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

/**
 * fastjson2 拦截器，允许将非标准 JSON 转换为标准 JSON，即对象 JSON 表示，以支持更灵活的 Yaml/Json 写法。
 * 或者是将标准 JSON 转为对象。
 * <p>
 * 比如将 Groot 作为自动化测试平台或造数平台的执行引擎，平台用例数据存在一些非执行数据（比如 header 是否启用等），
 * 可以利用该拦截器，将平台用例数据转换为标准的 Groot 用例数据。
 */
public interface FastJson2Interceptor {

    /**
     * important! 暂不支持。
     * 如果不需要转换，方法约定返回 null，同时 JSON 反序列化将使用 value，否则使用返回对象。
     *
     * @param clazz
     * @param value
     * @return
     */
    <T extends ConfigItem<?>> Map<String, Object> deserializeConfigItem(Class<T> clazz, Object value);

    /**
     * 如果不需要转换，方法约定返回 null，同时 JSON 反序列化将使用 value，否则使用返回对象。
     *
     * @param clazz
     * @param value TestElement JSON 表示
     * @return
     */
    <T extends TestElement<?>> Map<String, Object> deserializeTestElement(Class<T> clazz, Map<String, Object> value);

    /**
     * 如果不需要转换，方法约定返回 null，同时 JSON 反序列化将使用 value，否则使用返回对象。
     *
     * @param clazz
     * @param value
     * @return
     */
    <T extends PreProcessor> Map<String, Object> deserializePreProcessor(Class<T> clazz, Object value);

    /**
     * 如果不需要转换，方法约定返回 null，同时 JSON 反序列化将使用 value，否则使用返回对象。
     *
     * @param clazz
     * @param value
     * @return
     */
    <T extends PostProcessor> Map<String, Object> deserializePostProcessor(Class<T> clazz, Object value);

    /**
     * @param clazz        null 值表示 null 或空列表
     * @param type
     * @param matcherKey
     * @param matcherValue
     * @return
     */
    Matcher deserializeMatcher(List<Class> clazz, List<String> type, String matcherKey, Object matcherValue);

    String serialize();

}
