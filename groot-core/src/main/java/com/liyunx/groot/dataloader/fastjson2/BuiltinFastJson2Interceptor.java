package com.liyunx.groot.dataloader.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.liyunx.groot.matchers.ProxyMatchers;
import com.liyunx.groot.processor.HooksPostProcessor;
import com.liyunx.groot.processor.HooksPreProcessor;
import com.liyunx.groot.processor.PostProcessor;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.processor.assertion.standard.StandardAssertion;
import com.liyunx.groot.processor.extractor.standard.StandardExtractor;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.controller.ForEachController;
import com.liyunx.groot.testelement.controller.WhileController;
import com.liyunx.groot.util.CollectionUtil;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.dataloader.fastjson2.deserializer.MatcherObjectReader.TYPE_KEY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * 内置的 fastjson2 拦截器：处理 core 包相关类
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BuiltinFastJson2Interceptor extends AbstractFastJson2Interceptor {

    @Override
    public <T extends TestElement<?>> Map<String, Object> deserializeTestElement(Class<T> clazz, Map<String, Object> value) {

        // ++ WhileController ++
        if (WhileController.class.equals(clazz)) {
            Object whileObject = value.get(WhileController.KEY);
            // while: ${cnt < 10}
            // <=>
            // while:
            //   condition: ${cnt < 10}
            if (whileObject instanceof String) {
                Map<String, Object> map = new HashMap<>();
                map.put("condition", whileObject);
                value.put(WhileController.KEY, map);
                return value;
            }
            return null;
        }

        // ++ ForEachController ++
        if (ForEachController.class.equals(clazz)) {
            Object forObject = value.get(ForEachController.KEY);
            if (forObject instanceof Map forMap) {
                Object column = forMap.get("column");
                Object row = forMap.get("row");
                Object table = forMap.get("table");

                // column:
                //   username: ["tom", "jack"]
                //   password: ["tom_pwd", "jack_pwd"]
                if (column instanceof Map) {
                    // 列模式转行模式
                    Map<String, List<Object>> map = (Map<String, List<Object>>) column;             //待转换的列模式数据集
                    int maxSize = map.values().stream().mapToInt(List::size).max().getAsInt();      //查找最大行数
                    List<Map<String, Object>> data = new ArrayList<>();                             //初始化行模式数据集：创建列表对象
                    CollectionUtil.fill(data, maxSize, HashMap::new);                                //初始化行模式数据集：使用空对象填充
                    for (Map.Entry<String, List<Object>> entry : map.entrySet()) {                  //列模式数据填充到行模式数据集
                        String key = entry.getKey();
                        List<Object> list = entry.getValue();
                        for (int i = 0; i < list.size(); i++) {
                            data.get(i).put(key, list.get(i));
                        }
                        for (int i = list.size(); i < data.size(); i++) {
                            data.get(i).put(key, null);
                        }
                    }
                    forMap.put("data", data);
                    return value;
                }

                // row:
                //   - username: tom
                //     password: tom_pwd
                //   - username: jack
                //     password: jack_pwd
                if (row instanceof List) {
                    forMap.put("data", row);
                    return value;
                }

                // table:
                //   - [username, password]
                //   - ["tom", "tom_pwd"]
                //   - ["jack", "jack_pwd"]
                if (table instanceof List && ((List) table).size() > 1) {
                    List<List<Object>> tableList = (List<List<Object>>) table;
                    List<Object> names = tableList.get(0);
                    if (names != null) {
                        //表格模式转行模式
                        List<String> namesList = names.stream().map(name -> (String) name).toList();
                        List<Map<String, Object>> data = new ArrayList<>();             //初始化行模式数据集：创建列表对象
                        CollectionUtil.fill(data, tableList.size() - 1, HashMap::new);      //初始化行模式数据集：使用空对象填充
                        for (int i = 1; i < tableList.size(); i++) {                    //values 数据集填充到行模式数据集
                            List<Object> list = tableList.get(i);                         //当前行 value 数据
                            int min = Math.min(list.size(), namesList.size());
                            Map<String, Object> data_i = data.get(i - 1);
                            for (int j = 0; j < min; j++) {
                                data_i.put(namesList.get(j), list.get(j));
                            }
                            for (int j = min; j < namesList.size(); j++) {
                                data_i.put(namesList.get(j), null);
                            }
                        }
                        forMap.put("data", data);
                        return value;
                    } else {
                        String builder = "ForEachController.for.table 的书写形式应如下所示：\n" +
                            "table:\n" +
                            "  - [username, password]\n" +
                            "  - [tom, tom_pwd]\n" +
                            "  - [jack, jack_pwd]";
                        throw new JSONException(builder);
                    }
                }

                return null;
            }
            return null;
        }

        return null;
    }


    @Override
    public <T extends PreProcessor> Map<String, Object> deserializePreProcessor(Class<T> clazz, Object value) {
        // ++ HooksPreProcessor ++
        if (HooksPreProcessor.class.equals(clazz)) {
            Map<String, Object> map = new HashMap<>();
            // 写法示例 >>
            // hooks: ${sum(10, 20)}
            if (value instanceof String) {
                ArrayList<String> list = new ArrayList<>();
                list.add((String) value);
                map.put("hooks", list);
                return map;
            }

            // 写法示例 >>
            // hooks:
            //   - ${sum(10, 20)}
            //   - ${sum(19, 23)}
            if (value instanceof List) {
                map.put("hooks", value);
                return map;
            }

            // 标准结构写法
            if (value instanceof Map m && m.size() == 1 && m.containsKey("hooks")) {
                return m;
            }

            throw new JSONException("HooksPreProcessor " +
                "当前仅支持三种写法：setup[Before|After]Hooks/setup[Before|After][*].hooks/setup.[before|after][*].hooks，" +
                "值有两种写法：string/list(string)");
        }

        return null;
    }

    @Override
    public <T extends PostProcessor> Map<String, Object> deserializePostProcessor(Class<T> clazz, Object value) {
        // ++ StandardExtractor ++
        if (StandardExtractor.class.isAssignableFrom(clazz) && value instanceof List list) {
            // 位置参数转键值对参数
            // 写法示例 >>
            // jsonpath: [id2, $.data.id]
            String refName = (String) list.get(0);
            String expression = (String) list.get(1);
            Map<String, Object> paramsOrDataMap = list.size() > 2 ? (Map<String, Object>) list.get(2) : new HashMap<>();
            paramsOrDataMap.put("refName", refName);
            paramsOrDataMap.put("expression", expression);
            return paramsOrDataMap;
        }

        // ++ StandardAssertion ++
        if (StandardAssertion.class.isAssignableFrom(clazz) && value instanceof List list) {
            // 位置参数转键值对参数
            // 写法示例 >>
            // equalTo: ["abc", "abc"]
            Object check = list.get(0);
            Object expect = list.get(1);
            Map<String, Object> paramsOrDataMap = list.size() > 2 ? (Map<String, Object>) list.get(2) : new HashMap<>();
            paramsOrDataMap.put("check", check);
            paramsOrDataMap.put("expect", expect);
            return paramsOrDataMap;
        }

        // ++ HooksPostProcessor ++
        if (HooksPostProcessor.class.equals(clazz)) {
            Map<String, Object> map = new HashMap<>();
            if (value instanceof String) {
                ArrayList<String> list = new ArrayList<>();
                list.add((String) value);
                map.put("hooks", list);
                return map;
            }

            if (value instanceof List) {
                map.put("hooks", value);
                return map;
            }

            // 标准结构写法
            if (value instanceof Map m && m.size() == 1 && m.containsKey("hooks")) {
                return m;
            }

            throw new JSONException("HooksPostProcessor " +
                "当前仅支持两种写法：teardownHooks/teardown[*].hooks" +
                "值有两种写法：string/list(string)");
        }

        return null;
    }

    @Override
    public Matcher deserializeMatcher(List<Class> clazz, List<String> type, String matcherKey, Object matcherValue) {
        // 是否需要可扩展，如果需要可扩展，可以使用策略模式改造
        // TODO 优化：没有表达式时，返回标准 Matcher？

        // 逻辑断言
        if ("allOf".equals(matcherKey)) {
            return ProxyMatchers.allOf(subMatchers(clazz, type, matcherValue));
        }
        if ("anyOf".equals(matcherKey)) {
            return ProxyMatchers.anyOf(subMatchers(clazz, type, matcherValue));
        }

        // 类型断言
        if ("equalTo".equals(matcherKey)) {
            return ProxyMatchers.equalTo(getFirst(clazz), String.valueOf(matcherValue));
        }

        // 值断言：String
        if ("containsString".equals(matcherKey)) {
            return ProxyMatchers.containsString(String.valueOf(matcherValue));
        }

        return null;
    }

    private static Class getFirst(List<Class> clazz) {
        return isNull(clazz) ? null : clazz.get(0);
    }

    private Iterable subMatchers(List<Class> clazz, List<String> type, Object matcherValue) {
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
