package com.liyunx.groot.config.builtin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.ConfigItem;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 变量配置项。
 *
 * <p>important! 不要依赖变量声明顺序来完成一些任务，比如：
 * <pre>
 *   variables:
 *     username: "tom"
 *     password: ${username + 'pwd'}
 * </pre>
 * 你可以在后续的前置处理器中完成该任务，比如：
 * <pre>
 *   variables:
 *     username: "tom"
 *     password: "pwd"
 *   setup_hooks:
 *     - ${vars.put("password", username + password)}
 * </pre>
 *
 * <p><br/>变量会在运行时修改，而全局变量和环境变量是线程共享的，故这里使用 ConcurrentHashMap。
 *
 * <p><br/>这里性能可以优化下，仍旧使用 HashMap，需要并发安全的地方，
 * 如生成全局上下文、环境上下文或 Environment 对象时转为 ConcurrentHashMap(ConcurrentVariableConfigItem)，
 * 即：VariableConfigItem 改为接口，继承 Map 和 ConfigItem 接口，有 DefaultVariableConfigItem 和 ConcurrentVariableConfigItem 两种实现。
 * 当然还可以暴力点，全局和环境配置使用另一个 key，如 variables2，但这样破坏了统一性，不应该使用。
 * 暂时不改了，目前以自动化为目标，不考虑性能测试，影响不大。
 *
 * <p>ConcurrentHashMap 还有一个问题，就是 Key 和 Value 都不能为 null，Key 是可以控制的，但 Value 可能为 null，比如提取器。
 *
 * TODO 这个地方的设计后面有时间再来优化，先完成主要功能
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@KeyWord(VariableConfigItem.KEY)
public class VariableConfigItem extends ConcurrentHashMap<String, Object> implements ConfigItem<VariableConfigItem> {

    public static final String KEY = "variables";

    public VariableConfigItem() {
    }

    public VariableConfigItem(Map<String, Object> variables) {
        super(variables);
    }


    @Override
    public ValidateResult validate() {
        // nothing to do.
        return new ValidateResult();
    }

    @Override
    public VariableConfigItem merge(VariableConfigItem other) {
        VariableConfigItem res = new VariableConfigItem();
        res.putAll(this);
        if (other != null)
            res.putAll(other);
        return res;
    }

    @Override
    public VariableConfigItem copy() {
        return variableCopy(this);
    }

    public static <T> T variableCopy(T source) {
        if (source instanceof Map) {
            Map res = getMapInstance((Map) source);
            ((Map<?, ?>) source).forEach((k, v) -> res.put(k, variableCopy(v)));
            return (T) res;
        }
        if (source instanceof List) {
            List res = getListInstance((List) source);
            ((List<?>) source).forEach(e -> res.add(variableCopy(e)));
            return (T) res;
        }
        // 其他类型直接返回：
        // 基本数据类型包装类：不可变类
        // String 类型：不可变类
        // Object 类型：共享对象
        // BeanSupplier 类型：每次循环时调用 get() 方法重新计算，如 var("personKey", () -> Person::new)
        //     BeanSupplier 应用场景：代码用例中，包装共享对象或每次返回新对象
        return source;
    }

    private static Map getMapInstance(Map source) {
        if (source instanceof VariableConfigItem) {
            return new VariableConfigItem();
        }
        if (source instanceof JSONObject) {
            return new JSONObject();
        }
        if (source instanceof ConcurrentHashMap) {
            return new ConcurrentHashMap();
        }
        if (source instanceof LinkedHashMap) {
            return new LinkedHashMap();
        }
        if (source instanceof HashMap) {
            return new HashMap();
        }
        try {
            return source.getClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            return new LinkedHashMap(source);
        }
    }

    private static List getListInstance(List source) {
        if (source instanceof JSONArray) {
            return new JSONArray();
        }
        if (source instanceof ArrayList) {
            return new ArrayList();
        }
        if (source instanceof LinkedList) {
            return new LinkedList();
        }
        try {
            return source.getClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            return new ArrayList(source);
        }
    }

    /**
     * 变量配置 Builder
     */
    public static class Builder {

        VariableConfigItem variableConfigItem = new VariableConfigItem();

        public Builder var(String name, Object value){
            variableConfigItem.put(name, value);
            return this;
        }

        public VariableConfigItem build(){
            return variableConfigItem;
        }

    }

}
