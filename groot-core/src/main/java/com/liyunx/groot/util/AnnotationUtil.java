package com.liyunx.groot.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

public class AnnotationUtil {

    /**
     * 判断一个注解是否包含目标注解
     *
     * @param type       要检查的注解
     * @param targetType 目标注解
     * @return 如果注解等于或包含目标注解，返回 true；否则返回 false
     */
    public static boolean hasAnnotation(Class<? extends Annotation> type, Class<? extends Annotation> targetType) {
        if (type == targetType) {
            return true;
        }
        for (Annotation annotation : type.getDeclaredAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!isJdkAnnotation(annotationType)) {
                if (hasAnnotation(annotationType, targetType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断某个注解是否是 JDK 内置的注解
     *
     * @param annotation 要检查的注解
     * @return 是否是 JDK 内置的注解
     */
    private static boolean isJdkAnnotation(Class<? extends Annotation> annotation) {
        String packageName = annotation.getPackage().getName();
        return packageName.equals("java.lang.annotation");
    }

    /**
     * <p><b>from https://github.com/leangen/geantyref/</b></p>
     * <p>
     * Creates an instance of an annotation.
     *
     * @param annotationType The {@link Class} representing the type of the annotation to be created.
     * @param values         A map of values to be assigned to the annotation elements.
     * @param <A>            The type of the annotation.
     * @return An {@link Annotation} instanceof matching {@code annotationType}
     * @throws AnnotationFormatException Thrown if incomplete or invalid {@code values} are provided
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A newInstance(Class<A> annotationType, Map<String, Object> values)
        throws AnnotationFormatException {
        return (A) Proxy.newProxyInstance(annotationType.getClassLoader(),
            new Class[]{annotationType},
            new AnnotationInvocationHandler(annotationType, values == null ? Collections.emptyMap() : values));
    }

}
