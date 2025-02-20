package com.liyunx.groot.template;

import com.liyunx.groot.common.Computable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.support.BeanSupplier;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 模板引擎：根据上下文计算模板的结果。
 *
 * <p>
 * 模板一般是插值字符串，如 ${sum(10, 20)}。
 *
 * <p>
 * 内置的默认模板引擎采用 FreeMarker。可以按需实现自己的模板引擎。
 */
public interface TemplateEngine {

    Pattern EXPRESSION = Pattern.compile("[\\s\\S]*\\$\\{.+}[\\s\\S]*");

    /**
     * 计算模板结果，不包含任何内置函数、内置变量。
     *
     * @param model 数据模型
     * @param text  模板字符串
     * @return 模板计算结果
     */
    Object eval(Map<String, Object> model, String text);

    /**
     * 计算模板结果，包含了内置函数、内置变量。
     *
     * @param context 测试上下文
     * @param text    模板字符串
     * @return 模板计算结果
     */
    Object eval(ContextWrapper context, String text);

    default Map<String, String> eval(ContextWrapper ctx, Map<String, String> map) {
        if (map == null)
            return null;

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            map.put(k, String.valueOf(eval(ctx, v)));
        }
        return map;
    }

    default List<String> eval(ContextWrapper ctx, List<String> list) {
        if (list == null)
            return null;

        for (int i = 0; i < list.size(); i++) {
            list.set(i, String.valueOf(eval(ctx, list.get(i))));
        }
        return list;
    }

    /**
     * 计算对象中的表达式，替换为表达式的值。
     *
     * <p>当对象类型为：
     * <ul>
     *     <li>String 类型：计算后返回</li>
     *     <li>Computable 类型：调用 eval(ctx) 方法后返回</li>
     *     <li>BeanSupplier 类型：调用 get() 后返回</li>
     *     <li>List/Map 类型：原地计算，更新替换原来的值</li>
     *     <li>其他的 Bean 类型：原样返回</li>
     * </ul>
     *
     * @param obj 可能包含模板字符串的对象
     * @return 完成模板计算后的对象（Map/List 会原地更新）
     */
    default Object eval(ContextWrapper ctx, Object obj) {
        if (obj instanceof Map) {
            Map map = (Map) obj;
            map.forEach((key, value) -> {
                // tips: 值类型可能会发生变更，导致赋值时 ClassCastException
                // 如果使用了 eval 方法，应当注意到这一点，List 同理
                //
                // 示例：
                // Map<String, String> map = new HashMap<String, String>(){{
                //   put("k1", "${toInt('88')}");
                // }};
                // eval(map);
                // String value = map.get("k1");
                //
                // String value = map.get("k1") 这句代码将会报错：实际类型和预期类型不一致
                // java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
                //
                // 为了类型安全，应当：
                // String value = String.valueOf(map.get("k1"));
                // 或者对 map 进行类型检查和转换
                // 当然，Groot 中变量是 <String, Object> 类型，不存在类型转换问题，其他场景同理
                //
                // tips：这里无法获取 map 使用时的泛型声明类型（编译时类型）
                // 如果是 MyMap2 extends MyMap<String>, MyMap<V> extends HashMap<String, V> 这样的情形，是可以获取泛型类型的，
                // 但如果是 Map<String, String> map = new HashMap<>(); 这样的情形，则无法获取使用时的泛型类型，因此此处无法做统一处理。
                // 为了方便，对常见类型，如 Map<String, String>、List<String> 进行了方法重载。
                map.put(key, eval(ctx, value));
            });
            return map;
        } else if (obj instanceof List) {
            List list = (List) obj;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, eval(ctx, list.get(i)));
            }
            return list;
        } else if (obj instanceof String) {
            return eval(ctx, (String) obj);
        } else if (obj instanceof BeanSupplier) {       // 每次循环重新计算
            return ((BeanSupplier<?>) obj).get();
        } else if (obj instanceof Computable) {
            return ((Computable<?>) obj).eval(ctx);
        } else {
            // 以下情况直接返回：null/基本数据类型/Bean 对象
            // obj == null
            // obj instanceof Number
            // obj instanceof Boolean
            // obj instanceof Character
            // obj is Bean Object
            return obj;
        }
    }

    /**
     * 判断模板中是否包含 ${} 插值表达式
     *
     * @param text 模板字符串
     * @return 如果包含插值表达式，返回 true，否则返回 false
     */
    static boolean hasExpression(String text) {
        // 后续如果要扩展，可以放到配置中，从 Configuration 中读取，
        // 暂时不考虑支持其他形式写法，如 {{username}}，该写法在 Yaml 中必须加双引号，否则和 Yaml 语法冲突
        // 这里之所以是静态方法，而非 TemplateEngine 的接口方法，是因为该方法可能在非用例执行时调用，
        // 此时 TemplateEngine 实例还未创建，比如调用 validate() 方法
        return EXPRESSION.matcher(text).matches();
    }

}
