package com.liyunx.groot.support;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;

import static com.liyunx.groot.constants.GrootConstants.GROOT_BASE_PACKAGE_NAME;

/**
 * Groot Service 加载，如测试元件/前后置处理器/断言/提取器等关键字字典、函数等。
 */
public class GrootServiceLoader {

    private static final Logger log = LoggerFactory.getLogger(GrootServiceLoader.class);

    // 类加载方式：SPI 或类扫描
    private enum LoaderType {
        SPI("spi"),
        SCAN_PACKAGE("package");

        private final String value;

        LoaderType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * 通过 SPI 查找并返回指定接口或抽象类的所有实现类
     *
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 指定接口或抽象类的所有实现类
     */
    public static <T> List<T> loadAsListBySPI(Class<T> clazz) {
        return loadWithLog(clazz.getSimpleName(), LoaderType.SPI, () -> {
            // SPI 查找并注册
            List<T> list = new ArrayList<>();
            ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz);
            serviceLoader.iterator().forEachRemaining(list::add);
            return list;
        });
    }

    /**
     * 通过包扫描查找并返回指定接口或抽象类的所有实现类
     *
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 指定接口或抽象类的所有实现类
     */
    public static <T> List<T> loadAsListByScanPackage(Class<T> clazz) {
        return loadWithLog(clazz.getSimpleName(), LoaderType.SCAN_PACKAGE, () -> {
            List<T> list = new ArrayList<>();
            Set<Class<? extends T>> classSet = ReflectUtil.scanImplTypes(GROOT_BASE_PACKAGE_NAME, clazz);
            classSet.forEach(implClass -> {
                try {
                    list.add(implClass.getConstructor().newInstance());
                } catch (NoSuchMethodException |
                         IllegalAccessException |
                         InstantiationException |
                         InvocationTargetException e) {
                    String errorMessage = String.format("类 %s 实例化失败", clazz.getCanonicalName());
                    log.error(errorMessage, e);
                }
            });
            return list;
        });
    }

    /**
     * 通过 SPI 查找并返回指定接口或抽象类的关键字字典
     *
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 指定接口或抽象类的关键字字典
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, Class<? extends T>> loadAsMapBySPI(Class<T> clazz) {
        return loadWithLog(clazz.getSimpleName(), LoaderType.SPI, () -> {
            // SPI 查找并注册
            Map<String, Class<? extends T>> keyMap = new HashMap<>();
            ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz);
            serviceLoader.iterator().forEachRemaining(t -> {
                Class<? extends T> implClazz = (Class<? extends T>) t.getClass();
                String key = getKeyWord(implClazz);
                // 存在 KeyWord 注解，并且有值的情况下，进行注册
                if (key != null) keyMap.put(key, implClazz);
            });
            return Collections.unmodifiableMap(keyMap);
        });
    }

    /**
     * 通过包扫描查找并返回指定接口或抽象类的关键字字典
     *
     * <p>未将包扫描作为默认加载方式，用户可自行使用。包扫描耗时比 SPI 长，会增加启动时间，不适合经常调试 TestNG/Junit 单个用例的开发场景。
     *
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 指定接口或抽象类的关键字字典
     */
    public static <T> Map<String, Class<? extends T>> loadAsMapByScanPackage(Class<T> clazz) {
        return loadWithLog(clazz.getSimpleName(), LoaderType.SCAN_PACKAGE, () -> {
            Set<Class<? extends T>> classSet = ReflectUtil.scanImplTypes(GROOT_BASE_PACKAGE_NAME, clazz);
            Map<String, Class<? extends T>> keyMap = new HashMap<>();
            classSet.forEach(implClazz -> {
                String key = getKeyWord(implClazz);
                // 存在 KeyWord 注解，并且有值的情况下，进行注册
                if (key != null) keyMap.put(key, implClazz);
            });
            return Collections.unmodifiableMap(keyMap);
        });
    }

    // Debug 日志
    private static <T> T loadWithLog(String className, LoaderType type, Supplier<T> supplier) {
        long start = 0;
        if (log.isDebugEnabled())
            start = System.currentTimeMillis();

        // 执行 Service 加载逻辑，获取加载结果
        T result = supplier.get();

        if (log.isDebugEnabled())
            log.debug("{} 扫描耗时：{} ms [by {}]", className, System.currentTimeMillis() - start, type);

        return result;
    }

    // 获取目标类的 @KeyWord 注解并返回它的值
    private static String getKeyWord(Class<?> clazz) {
        KeyWord annotation = clazz.getAnnotation(KeyWord.class);
        // 如果缺失 @KeyWord 注解，则跳过
        if (annotation == null) {
            log.warn("Class {} miss KeyWord Annotation, JSON Key is unknown, this class is not registered.",
                clazz.getCanonicalName());
            return null;
        }

        // 不需要进行注册的类，直接跳过
        if (annotation.ignore()) return null;

        // 如果 Key 值缺失，则跳过
        String key = annotation.value();
        if (KeyWord.miss.equals(key)) {
            log.warn("Class {} KeyWord Annotation value is missing, this class is not registered.",
                clazz.getCanonicalName());
            return null;
        }
        return key;
    }

}
