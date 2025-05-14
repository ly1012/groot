package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.processor.assertion.Assertion;
import com.liyunx.groot.processor.assertion.matchers.MatcherAssertion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.dataloader.fastjson2.deserializer.MatcherObjectReader.TYPE_KEY;
import static com.liyunx.groot.processor.assertion.matchers.MatcherAssertion.*;
import static java.util.Objects.nonNull;

/**
 * Assertion JSON String to Assertion Object
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AssertionObjectReader implements ObjectReader<Assertion> {

    private static final String EQUAL_TO = "equalTo";

    @Override
    public Assertion readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        // 尝试获取当前测试元件的关键字，并对断言 KeyWord 标准化处理，无法从方法入参直接或间接拿到
        //
        // 方法一：反射访问非公开字段，依赖性太高，存在风险，比如第三方变更或其他 JSONReader 实现类没有 str 字段
        //try {
        //    Field strField = jsonReader.getClass().getDeclaredField("str");
        //    strField.setAccessible(true);
        //    String str = (String) strField.get(jsonReader);
        //    strField.setAccessible(false);
        //    System.out.println(str);
        //} catch (NoSuchFieldException | IllegalAccessException e) {
        //    throw new RuntimeException(e);
        //}
        //
        // 方法二：使用 ThreadLocal 传递数据
        // 无法保证反序列化时，字段的解析顺序，除非使用栈结构而非 String 类型进行存储，但这会增加代码量，不如第三种方法
        // for:          -> 1: for(set) ✅
        //   steps:
        //     http:     -> 2: http(set) ✅
        //   validate:   -> 3: http(get) ❌ should be for
        //
        //方法三：在 TestElementObjectReader 中处理（可行）

        // 读取断言数据
        Map<String, Object> assertMap = jsonReader.readObject();
        Map.Entry<String, Object> entry = assertMap.entrySet().stream().findFirst().get();
        String key = entry.getKey();
        Object value = entry.getValue();

        // 读取断言类型
        Map<String, Class<? extends Assertion>> keyMap = ApplicationConfig.getAssertionKeyMap();
        Class<? extends Assertion> clazz = keyMap.get(key);
        if (clazz == null)
            throw new JSONException(String.format("%s 不是支持的断言类型", key));

        // 原始数据转标准 JSON 数据
        Map<String, Object> dataMap = null;
        try {
            dataMap = PostProcessorObjectReader.getPostProcessorData(clazz, value);
        } catch (GrootException exception) {
            // 抛异常说明返回值为 null 且 value 不是 Map 类型（即断言实现方没有提供简写转标准 JSON 的 FastJson2Interceptor）
            if (!MatcherAssertion.class.isAssignableFrom(clazz)) {
                throw exception;
            }
            // 当 clazz 是 MatcherAssertion 子类，尝试转换为标准 JSON
            // 不适用于增加了新字段的 MatcherAssertion 子类，比如 HttpHeaderMatcherAssertion，这种情况转换会遗漏新字段
            // 形如 statusCode: 200，默认使用 equalTo 匹配
            if (isPrimitiveOrStringType(value.getClass().getName())) {
                dataMap = new HashMap<>();
                List<Map<String, Object>> matchers = new ArrayList<>();
                dataMap.put(MATCHERS_KEY, matchers);
                Map<String, Object> matcherMap = new HashMap<>();
                matcherMap.put(EQUAL_TO, value);
                matchers.add(matcherMap);
            }
            // statusText:
            //   - equalTo: "OK"
            //   - containsString: "K"
            else if (value instanceof List list
                && !list.isEmpty()
                && list.get(0) instanceof Map) {

                dataMap = new HashMap<>();
                dataMap.put(MATCHERS_KEY, list);
            } else {
                throw exception;
            }
        }

        // 根据 MatcherAssertion 类型，补充参数值默认类型信息
        if (MatcherAssertion.class.isAssignableFrom(clazz)) {
            // 为了解决 fastjson2 高版本的问题，自动将 mapper 改为 mapping，见 MatcherAssertion mapper 字段注释
            if (nonNull(dataMap.get(MAPPER_KEY))) {
                dataMap.put(MAPPING_KEY, dataMap.get(MAPPER_KEY));
                dataMap.remove(MAPPER_KEY);
            }

            Object type = null;
            // 1. 优先使用指定 type
            if (dataMap.get(TYPE_KEY) != null) {
                type = dataMap.get(TYPE_KEY);
            }
            // 如果未指定 type
            else {
                Object mapper = dataMap.get(MAPPING_KEY);
                // 2. 其次，当 mapper 为基本类型或 String 类型，优先使用 mapper
                if (nonNull(mapper)) {
                    String typeFromMapper = null;
                    if (mapper instanceof String _mapper) {
                        typeFromMapper = _mapper;
                    } else if (mapper instanceof List _mapper) {
                        Object last = _mapper.get(_mapper.size() - 1);
                        if (last instanceof String) {
                            typeFromMapper = (String) last;
                        }
                    }
                    if (isPrimitiveOrStringType(typeFromMapper)) {
                        type = typeFromMapper;
                    }
                }
                // 3. 最后当 type 和 mapper 都未指定时，默认使用注解
                else {
                    Class<?> classFromAnnotation = ApplicationConfig.getAssertionValueTypeMap().get(key);
                    if (classFromAnnotation != null) {
                        String typeFromAnnotation = classFromAnnotation.getName();
                        if (isPrimitiveOrStringType(typeFromAnnotation)) {
                            type = typeFromAnnotation;
                        }
                    }
                }
            }

            dataMap.remove(TYPE_KEY);
            if (isTypeValueValid(type)) {
                List matchers = (List) dataMap.get(MATCHERS_KEY);
                for (Object matcher : matchers) {
                    if (matcher instanceof Map) {
                        ((Map) matcher).putIfAbsent(TYPE_KEY, type);
                    }
                }
            }
        }

        // Assertion JSON 表示转 Assertion 对象
        return JSON.parseObject(JSON.toJSONString(dataMap), clazz);
    }

    private boolean isTypeValueValid(Object value) {
        return nonNull(value) && (                                   // 非 Null，且为
            (value instanceof String str && !str.trim().isEmpty())   // 非空字符串或
                || (value instanceof List list && !list.isEmpty())   // 非空列表
        );
    }

    private boolean isPrimitiveOrStringType(String type) {
        return "byte".equals(type)
            || "java.lang.Byte".equals(type)
            || "short".equals(type)
            || "java.lang.Short".equals(type)
            || "int".equals(type)
            || "java.lang.Integer".equals(type)
            || "long".equals(type)
            || "java.lang.Long".equals(type)
            || "float".equals(type)
            || "java.lang.Float".equals(type)
            || "double".equals(type)
            || "java.lang.Double".equals(type)
            || "boolean".equals(type)
            || "java.lang.Boolean".equals(type)
            || "char".equals(type)
            || "java.lang.Character".equals(type)
            || "string".equals(type)
            || "java.lang.String".equals(type);
    }


}
