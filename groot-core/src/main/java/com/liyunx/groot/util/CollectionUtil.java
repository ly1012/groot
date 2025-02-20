package com.liyunx.groot.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 集合工具类
 */
public class CollectionUtil {

    /**
     * 使用 Supplier 填充列表 n 次。
     *
     * @param list     被填充的列表
     * @param <T>      元素类型
     * @param n        填充次数
     * @param supplier 元素生成器
     */
    public static <T> void fill(List<T> list, int n, Supplier<T> supplier) {
        for (int i = 0; i < n; i++) {
            list.add(supplier.get());
        }
    }

    /**
     * 数组转列表
     *
     * @param elements 数组元素
     * @param <T>      元素类型
     * @return 数组对应的列表表示
     */
    public static <T> List<T> listOf(T... elements) {
        return Arrays.stream(elements).collect(Collectors.toList());
    }

}
