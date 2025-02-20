package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;
import com.liyunx.groot.exception.GrootException;
import org.hamcrest.Matcher;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MatcherObjectReader implements ObjectReader<Matcher> {

    public static final MatcherObjectReader singleInstance = new MatcherObjectReader();

    private static final Map<String, Class> PRIMITIVE_OR_STRING_TYPE_MAP = new HashMap<>() {{
        put("byte", Byte.class);
        put("java.lang.Byte", Byte.class);
        put("short", Short.class);
        put("java.lang.Short", Short.class);
        put("int", Integer.class);
        put("java.lang.Integer", Integer.class);
        put("long", Long.class);
        put("java.lang.Long", Long.class);
        put("float", Float.class);
        put("java.lang.Float", Float.class);
        put("double", Double.class);
        put("java.lang.Double", Double.class);
        put("boolean", Boolean.class);
        put("java.lang.Boolean", Boolean.class);
        put("char", Character.class);
        put("java.lang.Character", Character.class);
        put("string", String.class);
        put("java.lang.String", String.class);
    }};

    @Override
    public Matcher readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 三个要素：默认值类型、Matcher 关键字、Matcher 数据
        String type = null;
        Class clazz = null;
        String matcherKey;
        Object matcherValue = null;

        Object matcherJsonData = jsonReader.readAny();
        if (matcherJsonData instanceof String) {
            matcherKey = (String) matcherJsonData;
        } else if (matcherJsonData instanceof Map) {
            Map<String, Object> matcherMap = (Map<String, Object>) matcherJsonData;
            type = (String) matcherMap.get("type");
            clazz = typeToClass(type);
            matcherMap.remove("type");
            Map.Entry<String, Object> dataEntry = matcherMap.entrySet().iterator().next();
            matcherKey = dataEntry.getKey();
            matcherValue = dataEntry.getValue();
        } else {
            throw new GrootException("用例格式非法，matchers 列表项仅支持 String 或 Map 类型，当前值：%s", JSON.toJSONString(matcherJsonData));
        }

        return dataToMatcher(clazz, type, matcherKey, matcherValue);
    }

    private Class typeToClass(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }

        Class primitiveOrStringClass = PRIMITIVE_OR_STRING_TYPE_MAP.get(type);
        if (primitiveOrStringClass != null) {
            return primitiveOrStringClass;
        }

        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Matcher dataToMatcher(Class clazz, String type, String matcherKey, Object matcherValue) {
        // 反序列化 Matcher，拦截器链式处理
        Matcher matcher = null;
        List<FastJson2Interceptor> interceptors = ApplicationConfig.getFastJson2Interceptors();
        for (FastJson2Interceptor interceptor : interceptors) {
            matcher = interceptor.deserializeMatcher(clazz, type, matcherKey, matcherValue);
            if (matcher != null) break;
        }
        if (matcher == null) {
            throw new UnsupportedOperationException(String.format("当前不支持 [%s] Matcher", matcherKey));
        }
        return matcher;
    }

}
