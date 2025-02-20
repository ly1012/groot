package com.liyunx.groot.support;

import java.util.function.Supplier;

@FunctionalInterface
public interface BeanSupplier<T> extends Supplier<T> {

    /**
     * 返回 BeanSupplier 参数本身，用于便捷创建 BeanSupplier 对象
     *
     * <pre>{@code
     *     HashMap<String, Object> map = new HashMap<>();
     *     map.put("obj", ofSupplier(Object::new));
     * }</pre>
     *
     * @param supplier BeanSupplier 对象
     * @param <T>      BeanSupplier 返回值类型
     * @return 返回参数本身
     */
    static <T> BeanSupplier<T> ofSupplier(BeanSupplier<T> supplier) {
        return supplier;
    }

}
