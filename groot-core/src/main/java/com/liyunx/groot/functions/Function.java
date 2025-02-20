package com.liyunx.groot.functions;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.template.freemarker.FreeMarkerTemplateEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一的函数接口，函数实现类至少应该具有一个无参构造器（用于反射实例化）。
 *
 * <p>函数实例仅注册一次，所以务必保证类是线程安全的。
 *
 * <p>1. 在表达式中使用函数
 * <blockquote><pre>
 * ${sum(10, 20)}
 *
 * // person 是一个变量，值为 Person 类的实例
 * ${func1(person)}
 * </pre></blockquote>
 *
 *
 * <p>2. 在代码中使用函数
 * <blockquote><pre>
 * // 直接调用（静态方法）
 * long res = SumFunction.sum(10, 20);
 * </pre></blockquote>
 *
 * <p>
 * 模板引擎必须兼容 Function。
 * 比如 FreeMarker 自定义函数的参数是 TemplateModel 类型（即包装类型，而非表达式中原始的 Object），
 * 需要转换为原始 Object 后调用 Function 实现类。
 *
 * @see FreeMarkerTemplateEngine
 */
public interface Function {

    /**
     * 用于表达式的函数名称，必须全局唯一。
     *
     * @return 全局唯一的函数名称
     */
    String getName();

    /**
     * 执行函数（不依赖上下文）。
     *
     * @param parameters 函数参数（可变长参数）
     * @return 函数执行结果
     * @see #execute(ContextWrapper, List)
     */
    default Object execute(Object... parameters) {
        List<Object> list = new ArrayList<>();
        Collections.addAll(list, parameters);
        return execute(null, list);
    }

    /**
     * 执行函数（不依赖上下文）。
     *
     * @param parameters 函数参数（列表类型参数）
     * @return 函数执行结果
     * @see #execute(ContextWrapper, List)
     */
    default Object execute(List<Object> parameters) {
        return execute(null, parameters);
    }

    /**
     * 执行函数（依赖上下文）。
     *
     * @param contextWrapper 执行上下文，可以为 null
     * @param parameters     函数参数（可变长参数）
     * @return 函数执行结果
     * @see #execute(ContextWrapper, List)
     */
    default Object execute(ContextWrapper contextWrapper, Object... parameters) {
        List<Object> list = new ArrayList<>();
        Collections.addAll(list, parameters);
        return execute(contextWrapper, list);
    }

    /**
     * 执行函数（依赖上下文）。
     *
     * <p>函数执行前应当先检查参数数量和参数类型。
     * 比如 sum(x, y) 函数接收两个参数，调用者可能的写法：
     *
     * <pre><code>
     * sum(10, 20)
     * sum("10", "20")
     * sum(a, b) (String a = "10", BigDecimal b = new BigDecimal(20) )
     * </code></pre>
     *
     * 当参数无法转换为需要的类型时，应当抛出异常。
     *
     * @param contextWrapper 执行上下文，可以为 null
     * @param parameters     函数参数（列表类型参数）
     * @return 函数执行结果
     */
    Object execute(ContextWrapper contextWrapper, List<Object> parameters);

}
