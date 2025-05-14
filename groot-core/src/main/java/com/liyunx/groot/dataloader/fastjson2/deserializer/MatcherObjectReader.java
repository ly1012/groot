package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.dataloader.DataLoadException;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;
import com.liyunx.groot.exception.GrootException;
import org.hamcrest.Matcher;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MatcherObjectReader implements ObjectReader<Matcher> {

    public static final String TYPE_KEY = "type";

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
        // 三个要素：参数值的默认类型、Matcher 关键字、Matcher 数据
        List<String> type = null;   // Yaml 用例中声明的类型值（简写名称或全限定类名）
        List<Class> clazz = null;   // 默认类型对应的 Class 对象
        String matcherKey;
        Object matcherValue = null;

        Object matcherJsonData = jsonReader.readAny();
        if (matcherJsonData instanceof String) {
            matcherKey = (String) matcherJsonData;
        } else if (matcherJsonData instanceof Map) {
            Map<String, Object> matcherMap = (Map<String, Object>) matcherJsonData;
            type = typeAsList(matcherMap.get(TYPE_KEY));
            clazz = typeToClass(type);
            matcherMap.remove(TYPE_KEY);
            Map.Entry<String, Object> dataEntry = matcherMap.entrySet().iterator().next();
            matcherKey = dataEntry.getKey();
            matcherValue = dataEntry.getValue();
        } else {
            throw new GrootException("用例格式非法，matchers 列表项仅支持 String 或 Map 类型，当前值：%s", JSON.toJSONString(matcherJsonData));
        }

        return dataToMatcher(clazz, type, matcherKey, matcherValue);
    }

    private List<String> typeAsList(Object typeValue) {
        if (isNull(typeValue)) {
            return null;
        }
        if (typeValue instanceof String _type) {
            List<String> type = new ArrayList<>();
            type.add(_type);
            return type;
        }
        if (typeValue instanceof List _type) {
            return (List<String>) _type;
        }
        throw new DataLoadException("%s 值只能是 String 或 String 列表类型，当前值：%s", TYPE_KEY, JSON.toJSONString(typeValue));
    }

    private List<Class> typeToClass(List<String> typeList) {
        if (isNull(typeList) || typeList.isEmpty()) {
            return null;
        }

        List<Class> classList = new ArrayList<>();
        for (String type : typeList) {
            if ("auto".equals(type)) {
                classList.add(null);
                continue;
            }

            Class primitiveOrStringClass = PRIMITIVE_OR_STRING_TYPE_MAP.get(type);
            if (nonNull(primitiveOrStringClass)) {
                classList.add(primitiveOrStringClass);
                continue;
            }

            try {
                classList.add(Class.forName(type));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return classList;
    }

    public static Matcher dataToMatcher(List<Class> clazz, List<String> type, String matcherKey, Object matcherValue) {
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
