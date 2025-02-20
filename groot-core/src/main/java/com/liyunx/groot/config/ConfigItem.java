package com.liyunx.groot.config;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.dataloader.fastjson2.deserializer.ConfigItemObjectReader;
import com.liyunx.groot.util.KryoUtil;

/**
 * 配置项接口，最基本的配置单元。
 * <p>
 * TODO 配置元件中的函数是立即执行（和元件执行同时），还是延迟执行（和 Sampler 执行同时）？或者由用户决定（不太好）？
 * 当前方案：
 * - 变量配置：立即执行，当前元件执行时执行，子元件执行时不会再执行
 * - 非变量配置：延迟执行，比如 HttpConfig.headers 中的函数，每次 Http 请求都会计算一次
 * <p>
 * TODO 合并策略是值合并，还是保持对象引用呢？如果配置只读取不修改，是不需要每次进行值拷贝（深度复制）的。
 * <p>
 * TODO 前置后置元件是否应该设计成可继承？有无必要？
 * 设计方案一：
 * 1. config:
 *       setup:
 *           - hooks: 'xxx'
 *           - jdbc: 'xxx'
 *       extract:
 *           - jsonpath: xxx
 *       assert:
 *           - equalTo: xxx
 *       teardown:
 *           - hooks: 'xxx'
 * 2. 合并配置项内容到当前元件的前置后置属性，父级前后置元件排在前面，即优先执行父级的再执行本级的。
 * 3. 如果上面的设计还不能满足要求，对前后置元件接口增加优先级属性方法，0 为最高优先级，默认优先级 100？
 * 4. 或者执行顺序排序(最终前置元件集合/后置元件集合)的其他设计？
 *
 * @param <T> 自身类型
 */
@JSONType(deserializer = ConfigItemObjectReader.class)
public interface ConfigItem<T extends ConfigItem<T>> extends ConfigElement<T> {

    /**
     * 合并相同的配置，参数对象的值会覆盖当前对象的值，方法应返回一个新的对象。
     *
     * @param other 配置项
     * @return 合并后的配置项
     */
    T merge(T other);

    /**
     * 除了特殊的变量配置上下文是部分浅拷贝以外，其他的一般配置项都是深拷贝（值拷贝）。
     *
     * @return
     */
    @SuppressWarnings({"unchecked"})
    default T copy() {
        return KryoUtil.copy((T) this);
    }

    /**
     * 类型安全检查：当前对象与目标对象的类型，如果一致则通过，否则抛出 {@link IllegalArgumentException}
     *
     * @param item 要检查的对象
     */
    default void typeCheck(T item) {
        if (!this.getClass().isAssignableFrom(item.getClass())) {
            Class<?> clazz = item.getClass();
            String className = "";
            if (item.getClass().isAnonymousClass()) {
                className = clazz.getSuperclass().getName();
            } else {
                className = clazz.getName();
            }
            throw new IllegalArgumentException(
                String.format("只有相同类型的配置才能合并。期望类型：%s，实际类型：%s",
                    this.getClass().getCanonicalName(), className)
            );
        }
    }

}
