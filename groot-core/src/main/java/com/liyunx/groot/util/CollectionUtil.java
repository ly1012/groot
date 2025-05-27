package com.liyunx.groot.util;

import java.util.*;
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
    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        return Arrays.stream(elements).collect(Collectors.toList());
    }


    /**
     * 判断 Map 是否为不可变 Map
     *
     * @param map Map
     * @return true 表示不可变 Map
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean isUnmodifiableMap(Map map) {
        // 先通过类名快速判断
        String className = map.getClass().getName();
        if (className.contains("UnmodifiableMap") ||
            className.startsWith("java.util.ImmutableCollections$") ||
            className.equals("com.google.common.collect.ImmutableMap")) {
            return true;
        }
        if (map instanceof HashMap || map instanceof TreeMap) {
            return false;
        }

        // 类名未知时，通过行为测试确认
        String testKey = "__groot_unmodifiable_test_key_389272435__";
        try {
            map.put(testKey, "");
            map.remove(testKey);
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    /**
     * 判断列表是否为不可变列表
     *
     * @param list 列表
     * @return true 表示不可变列表
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean isUnmodifiableList(List list) {
        // 先通过类名快速判断
        String className = list.getClass().getName();
        if (className.contains("UnmodifiableList") ||
            className.startsWith("java.util.ImmutableCollections$") ||
            className.equals("com.google.common.collect.ImmutableList")) {
            return true;
        }
        if (list instanceof ArrayList || list instanceof LinkedList) {
            return false;
        }

        // 类名未知时，通过行为测试确认
        try {
            list.add(0, "");
            list.remove(0);
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

}
