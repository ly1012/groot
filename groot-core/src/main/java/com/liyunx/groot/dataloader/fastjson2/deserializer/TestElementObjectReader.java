package com.liyunx.groot.dataloader.fastjson2.deserializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.dataloader.DataLoadException;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.processor.AbstractHooksProcessor;
import com.liyunx.groot.processor.extractor.standard.JsonPathExtractor;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.util.StringUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.constants.TestElementKeyWord.*;

/**
 * 测试元件解析器，将测试元件的 JSON 表示转为测试元件对象表示。
 * <p>
 * TestElement JSON String to TestElement Object
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestElementObjectReader implements ObjectReader<TestElement> {

    private static final String TEAR_DOWN_USAGE = TEAR_DOWN + " 节点用法：\n" +
        "(1) 在 key 上声明类型\n" +
        TEAR_DOWN + ":\n" +
        "    - " + EXTRACT + SEPARATOR + JsonPathExtractor.KEY + ": ['id', '$.data.id']\n" +
        "(2) 使用单独的 key 声明类型\n" +
        TEAR_DOWN + ":\n" +
        "    - " + TEAR_DOWN_TYPE + ": " + EXTRACT + "/" + VALIDATE + "/" + "default\n" +
        "      " + JsonPathExtractor.KEY + ": ['id', '$.data.id']\n";

    @Override
    public TestElement readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        Map<String, Object> testElementMap = jsonReader.readObject();
        return convertMapToTestElement(testElementMap);
    }

    // 将 TestElement 的 Map 表示转为对象表示
    private TestElement convertMapToTestElement(Map<String, Object> testElementMap) {
        // 查找符合条件的目标类和当前元件关键字
        Pair<Class<? extends TestElement>, String> classAndKeyPair = searchClassAndKeyByUniqueKeyWord(testElementMap);
        if (classAndKeyPair == null) {
            throw new JSONException(
                "No matching TestElement Class was found, JSON String: " + JSON.toJSONString(testElementMap));
        }
        Class<? extends TestElement> clazz = classAndKeyPair.getLeft();
        String elementKey = classAndKeyPair.getRight();

        // == 前后置简写语法转标准语法 ==

        // variables -> config.variables  覆盖原来的 config 节点（如果有）
        standardizeVariables(testElementMap);

        // setupBeforeHooks -> setupBefore[0].hooks  覆盖原来的 setupBefore 节点（如果有）
        standardizeHooks(testElementMap, SETUP_BEFORE);
        // setupAfterHooks -> setupAfter[0].hooks  覆盖原来的 setupAfter 节点（如果有）
        standardizeHooks(testElementMap, SETUP_AFTER);
        // teardownHooks -> teardown[0].hooks  覆盖原来的 teardown 节点（如果有）
        standardizeHooks(testElementMap, TEAR_DOWN);

        // setup:
        //   before:  -> setupBefore  覆盖原来的 setupBefore 节点（如果有）
        //   after:   -> setupAfter   覆盖原来的 setupAfter 节点（如果有）
        standardizeSetup(testElementMap);

        // TODO map -> list
        // extract:
        //  id: $.data.id
        //  token:
        //    jsonpath: $.data.token
        //standardizeExtractOrValidateMapOfTeardown(testElementMap);
        //standardizeExtractMap(testElementMap);
        //standardizeValidateMap(testElementMap);

        // teardown:
        //   - extract: list   -> [- extract$key]*   list 展开
        //   - validate: list  -> [- validate$key]*  list 展开
        standardizeExtractOrValidateListOfTeardown(testElementMap);

        // statusCode: 200 -> http$statusCode: 200  前后置处理器 key 修正
        // 简写转全写，如断言 statusCode: 200，完整写法为 http$statusCode: 200
        // Groot 支持每个 Sampler 有自己特定的前后置处理器，如断言，那么就会存在这样的情况：
        // HttpSampler 有一个 statusCode 断言，而另一个 FtpSampler 也有 statusCode 断言，
        // 如果断言中直接写 statusCode，则无法区分是那个协议的断言，所以标准写法或标准关键字应该是 http$statusCode 和 ftp$statusCode，
        // 但具体到一个 HTTP 或 FTP 请求，用户编写 Yaml/JSON 时则没必要加上协议关键字前缀，这属于冗余信息。
        // 所以这句代码的作用是我们根据协议关键字自动补全断言 Key。
        standardizeProcessorsKeyWord(testElementMap, elementKey);

        // 测试元件逻辑数据简写语法转标准语法，一般是关键字字段数据
        // 拦截器链式处理：如果需要转换，则使用指定拦截策略
        Map<String, Object> data = null;
        List<FastJson2Interceptor> interceptors = ApplicationConfig.getFastJson2Interceptors();
        for (FastJson2Interceptor interceptor : interceptors) {
            data = interceptor.deserializeTestElement(clazz, testElementMap);
            if (data != null)
                break;
        }
        if (data == null)
            data = testElementMap;

        String rawData = JSON.toJSONString(data);
        return JSON.parseObject(rawData, clazz);
    }

    private static void standardizeVariables(Map<String, Object> testElementMap) {
        String key = VariableConfigItem.KEY;

        // 如果没有 variables，直接返回
        Map<String, Object> variablesValue = (Map<String, Object>) testElementMap.get(key);
        if (variablesValue == null) {
            return;
        }

        // 否则，创建 config 节点并覆盖原来的 config 节点（如果存在）
        Map<String, Object> configValue = new HashMap<>();
        configValue.put(key, variablesValue);
        testElementMap.put("config", configValue);
        testElementMap.remove(key);
    }

    // name: setupBefore/setupAfter/teardown
    private static void standardizeHooks(Map<String, Object> testElementMap, String name) {
        // hooks -> Hooks
        String key = StringUtil.capitalize(AbstractHooksProcessor.KEY);

        // 如果没有 [name]Hooks，直接返回
        Object setupOrTeardownHooksValue = testElementMap.get(name + key);
        if (setupOrTeardownHooksValue == null) {
            return;
        }

        // 否则，创建 name 同名节点并覆盖原来的对应节点（如果存在）
        Map<String, Object> hooks = new HashMap<>();
        hooks.put(AbstractHooksProcessor.KEY, setupOrTeardownHooksValue);
        List<Map<String, Object>> setupOrTeardownValue = new ArrayList<>();
        setupOrTeardownValue.add(hooks);
        testElementMap.put(name, setupOrTeardownValue);
        testElementMap.remove(name + key);
    }

    private static void standardizeSetup(Map<String, Object> testElementMap) {
        Object _setupValue = testElementMap.get(SETUP);

        // 如果没有 setup 节点，直接返回
        if (_setupValue == null) {
            return;
        }

        // 否则，创建 setupBefore/setupAfter 节点并覆盖原来的节点（如果有）
        Map<String, Object> setupValue = (Map<String, Object>) _setupValue;
        Object beforeValue = setupValue.get(BEFORE);
        if (beforeValue != null) {
            testElementMap.put(SETUP_BEFORE, beforeValue);
        }
        Object afterValue = setupValue.get(AFTER);
        if (afterValue != null) {
            testElementMap.put(SETUP_AFTER, afterValue);
        }
        testElementMap.remove(SETUP);
    }

    private static void standardizeExtractOrValidateListOfTeardown(Map<String, Object> testElementMap) {
        Object _teardownValue = testElementMap.get(TEAR_DOWN);

        // 如果没有 teardown 节点，直接返回
        if (_teardownValue == null) {
            return;
        }

        // teardown:
        //	- extract:
        //		- jsonpath: [id, $.data.id]
        //	- validate:
        //		- statusCode: 200
        //		- equalTo: [$.response.status, 200]
        List<Map<String, Object>> teardownValue = (List<Map<String, Object>>) _teardownValue;
        List<Map<String, Object>> teardownValueCopy = new ArrayList<>();
        for (Map<String, Object> item : teardownValue) {
            if (item.size() != 1) {
                teardownValueCopy.add(item);
                continue;
            }

            // validate:
            //   - statusCode: 200
            //   - equalTo: [$.response.status, 200]
            Map.Entry<String, Object> extractOrValidateItem = item.entrySet().iterator().next();
            String key = extractOrValidateItem.getKey();
            Object _value = extractOrValidateItem.getValue();
            if (!EXTRACT.equals(key) && !VALIDATE.equals(key)) {
                teardownValueCopy.add(item);
                continue;
            }

            // - statusCode: 200
            // - equalTo: [$.response.status, 200]
            List<Map<String, Object>> value = (List) _value;
            for (Map<String, Object> processor : value) {
                if (processor.size() != 1) {
                    teardownValueCopy.add(processor);
                    continue;
                }
                // equalTo: [$.response.status, 200]
                Map.Entry<String, Object> processorItem = processor.entrySet().iterator().next();
                // validate$equalTo: [$.response.status, 200]
                Map<String, Object> standardItem = new HashMap<>();
                standardItem.put(key + SEPARATOR + processorItem.getKey(), processorItem.getValue());
                teardownValueCopy.add(standardItem);
            }

        }
        testElementMap.put(TEAR_DOWN, teardownValueCopy);
    }

    private static void standardizeProcessorsKeyWord(Map<String, Object> testElementMap, String elementKey) {
        Map<String, ?> preProcessorKeyMap = ApplicationConfig.getPreProcessorKeyMap();
        Map<String, ?> extractorKeyMap = ApplicationConfig.getExtractorKeyMap();
        Map<String, ?> assertionKeyMap = ApplicationConfig.getAssertionKeyMap();
        Map<String, ?> postProcessorKeyMap = ApplicationConfig.getPostProcessorKeyMap();

        List<Map<String, Object>> setupBefore = castToList(testElementMap, SETUP_BEFORE);
        List<Map<String, Object>> setupAfter = castToList(testElementMap, SETUP_AFTER);
        List<Map<String, Object>> extractors = castToList(testElementMap, EXTRACT);
        List<Map<String, Object>> assertions = castToList(testElementMap, VALIDATE);
        List<Map<String, Object>> teardown = castToList(testElementMap, TEAR_DOWN);

        handleProcessors(setupBefore, elementKey, preProcessorKeyMap);
        handleProcessors(setupAfter, elementKey, preProcessorKeyMap);
        handleProcessors(extractors, elementKey, extractorKeyMap);
        handleProcessors(assertions, elementKey, assertionKeyMap);
        handleTearDownProcessors(teardown, elementKey, extractorKeyMap, assertionKeyMap, postProcessorKeyMap);
    }

    private static List<Map<String, Object>> castToList(Map<String, Object> testElementMap, String key) {
        List<Map<String, Object>> value;
        try {
            value = (List<Map<String, Object>>) testElementMap.get(key);
        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put(key, testElementMap.get(key));
            throw new DataLoadException("Yaml/Json 用例中前后置处理器节点 {%s, %s, %s, %s, %s} 仅支持列表结构，当前数据结构：\n%s",
                SETUP_BEFORE, SETUP_AFTER, EXTRACT, VALIDATE, TEAR_DOWN,
                JSON.toJSONString(map, JSONWriter.Feature.PrettyFormat),
                e);
        }
        return value;
    }

    // 修正处理器 Key（前置/提取/断言）
    private static void handleProcessors(
        List<Map<String, Object>> target,
        String elementKey,
        Map<String, ?> processorKeyMap) {
        if (target != null) {
            // 处理所有处理器
            for (Map<String, Object> map : target) {
                standardizeSingleProcessorsKeyWord(map, elementKey, processorKeyMap);
            }
        }
    }

    private static void handleTearDownProcessors(
        List<Map<String, Object>> target,
        String elementKey,
        Map<String, ?> extractorKeyMap,
        Map<String, ?> assertionKeyMap,
        Map<String, ?> postProcessorKeyMap) {
        if (target != null) {
            // 处理所有处理器
            for (Map<String, Object> map : target) {
                // 解析获取：后置处理器类型、解析后的 Key
                String type;
                String processorKey;
                if (map.size() == 1) {
                    // 示例：
                    // teardown:
                    //   - extract$jsonpath: [id, '$.data.id']
                    //   - extract$http$jsonpath: [id, '$.data.id']
                    //   - myTearDown: 'ccc'
                    //   - http$myTearDown: 'ccc'
                    //   - notFound: ['', '']

                    // 处理某个处理器（Map 默认应该只有一个键值对）
                    // 获取当前处理器的 KeyWord：
                    // 可能为简写，可能为全写，也可能不存在（比如书写错误或未注册）
                    String rawProcessorKey = map.keySet().stream().findFirst().get();

                    // 解析 KeyWord 类型和实际 Key
                    // 可能为 extract/assert/default(teardown)
                    String[] parsedKey = parseKey(rawProcessorKey);
                    type = parsedKey[0];
                    processorKey = parsedKey[1];

                    if (!processorKey.equals(rawProcessorKey)) {
                        map.put(processorKey, map.get(rawProcessorKey));
                        map.remove(rawProcessorKey);
                    }
                } else if (map.size() == 2) {
                    // 获取类型 + 数据结构校验
                    Object typeObj = map.get(TEAR_DOWN_TYPE);
                    if (typeObj == null) {
                        throw new InvalidDataException(
                            TEAR_DOWN + " 节点数据结构非法，" + TEAR_DOWN_TYPE + " 节点不存在，当前结构：\n%s\n\n%s",
                            JSON.toJSONString(map, JSONWriter.Feature.PrettyFormat),
                            TEAR_DOWN_USAGE);
                    } else if (!(typeObj instanceof String)) {
                        throw new InvalidDataException(
                            TEAR_DOWN + ".[*]." + TEAR_DOWN_TYPE + " 节点的值必须是 String 类型\n当前类型：%s\n当前结构：\n%s\n\n%s",
                            typeObj.getClass().getCanonicalName(),
                            JSON.toJSONString(map, JSONWriter.Feature.PrettyFormat),
                            TEAR_DOWN_USAGE);
                    } else {
                        type = (String) typeObj;
                        if (!(type.equals(EXTRACT) || type.equals(VALIDATE) || type.equals("default"))) {
                            throw new InvalidDataException(
                                TEAR_DOWN + ".[*]." + TEAR_DOWN_TYPE + " 节点的值必须是以下三者之一：%s/%s/default\n当前结构：\n%s\n\n%s",
                                EXTRACT,
                                VALIDATE,
                                JSON.toJSONString(map, JSONWriter.Feature.PrettyFormat),
                                TEAR_DOWN_USAGE);
                        }
                    }

                    map.remove(TEAR_DOWN_TYPE);
                } else {
                    throw new InvalidDataException(TEAR_DOWN_USAGE + "\n当前结构：%s", JSON.toJSONString(map));
                }

                // 修正后置处理器的 Key
                Map<String, ?> processorKeyMap = switch (type) {
                    case EXTRACT -> extractorKeyMap;
                    case VALIDATE -> assertionKeyMap;
                    default -> postProcessorKeyMap;
                };
                standardizeSingleProcessorsKeyWord(map, elementKey, processorKeyMap);

                // 转为过渡结构（后置处理器 JSON 转对象时使用）
                // teardown:
                //   - type: extract/assert/default
                //     xxKey: xxx
                map.put(TEAR_DOWN_TYPE, type);
            }
        }
    }

    // 处理某个处理器（Map 默认应该只有一个键值对），修正单个处理器的关键字
    private static void standardizeSingleProcessorsKeyWord(
        Map<String, Object> target,
        String elementKey,
        Map<String, ?> processorKeyMap) {
        // 获取当前处理器的 KeyWord：可能为简写，可能为全写，也可能不存在（比如书写错误或未注册）
        String processorKey = target.keySet().stream().findFirst().get();
        // 如果不存在这样的处理器 KeyWord：可能为简写，如 statusCode，也可能不存在
        if (null == processorKeyMap.get(processorKey)) {
            // 但存在完整的处理器 KeyWord，如 http$statusCode，即简写情况
            String entireProcessorKey = elementKey + SEPARATOR + processorKey;
            if (processorKeyMap.get(entireProcessorKey) != null) {
                // 修正处理器 Key
                target.put(entireProcessorKey, target.get(processorKey));
                target.remove(processorKey);
            }
        }
    }

    private static String[] parseKey(String rawKey) {
        String[] res = new String[2];
        if (rawKey.startsWith(EXTRACT + SEPARATOR)) {
            res[0] = EXTRACT;
            res[1] = rawKey.substring((EXTRACT + SEPARATOR).length());
        } else if (rawKey.startsWith(VALIDATE + SEPARATOR)) {
            res[0] = VALIDATE;
            res[1] = rawKey.substring((VALIDATE + SEPARATOR).length());
        } else {
            res[0] = "default";
            res[1] = rawKey;
        }
        return res;
    }

    // 根据 TestElement 的特有 key 查找对应的 TestElement 类及其关键字
    // 如果未找到，返回 null
    private static Pair<Class<? extends TestElement>, String> searchClassAndKeyByUniqueKeyWord(Map<String, Object> testStepMap) {
        Map<String, Class<? extends TestElement>> keyMap = ApplicationConfig.getTestElementKeyMap();
        if (keyMap == null || keyMap.isEmpty())
            throw new JSONException(
                "TestElement JSON Key Map(Configuration.getTestElementKeyMap()) is empty, please register first.");
        for (Map.Entry<String, Object> entry : testStepMap.entrySet()) {
            String k = entry.getKey().toLowerCase();
            if (keyMap.containsKey(k)) {
                return new ImmutablePair<>(keyMap.get(k), k);
            }
        }
        return null;
    }

}
