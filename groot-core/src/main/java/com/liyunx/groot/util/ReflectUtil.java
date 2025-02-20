package com.liyunx.groot.util;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反射工具类
 */
public class ReflectUtil {

    /**
     * 查找指定包名下指定类型的所有子类实现类
     *
     * @param pkgName 包名，如：com.liyunx.groot.functions
     * @param type    目标类型
     * @param <T>     目标类型
     * @return 所有符合条件的子类实现类
     */
    public static <T> Set<Class<? extends T>> scanImplTypes(String pkgName, Class<T> type) {
        // 查找指定包下指定类型的所有子类
        Reflections reflections = new Reflections(pkgName);
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(type);
        // 过滤出实现类
        return subTypes.stream()
            .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
            .collect(Collectors.toSet());
    }

    /**
     * 查找指定包下包含指定注解的类
     *
     * @param pkgName        包名，如：com.liyunx.groot
     * @param annotationType 目标注解类型
     * @return 所有符合条件的子类实现类
     */
    public static Set<Class<?>> scanImplTypesByAnnotation(String pkgName, Class<? extends Annotation> annotationType) {
        // 查找指定包下包含指定注解的类
        Reflections reflections = new Reflections(pkgName);
        Set<Class<?>> subTypes = reflections.getTypesAnnotatedWith(annotationType);
        // 过滤出实现类
        return subTypes.stream()
            .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
            .collect(Collectors.toSet());
    }


}
