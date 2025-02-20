package com.liyunx.groot;

import com.liyunx.groot.annotation.MatcherValueType;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.dataloader.fastjson2.FastJson2Interceptor;
import com.liyunx.groot.dataloader.file.LocalDataLoader;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.filter.allure.AllureFilter;
import com.liyunx.groot.functions.Function;
import com.liyunx.groot.mapping.MappingFunction;
import com.liyunx.groot.model.ApplicationData;
import com.liyunx.groot.processor.PostProcessor;
import com.liyunx.groot.processor.PreProcessor;
import com.liyunx.groot.processor.assertion.Assertion;
import com.liyunx.groot.processor.assertion.matchers.MatcherAssertion;
import com.liyunx.groot.processor.extractor.Extractor;
import com.liyunx.groot.support.GrootServiceLoader;
import com.liyunx.groot.support.Worker;
import com.liyunx.groot.testelement.TestElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Groot 应用配置，这些配置在 Groot 实例之间共享，即一个 JVM 对应一套具体的 ApplicationConfig 配置。
 * <p>
 * {@link ApplicationConfig} 是应用共享配置，{@link Configuration} 是 Groot 实例化对象独享的配置。
 */
@SuppressWarnings("rawtypes")
public class ApplicationConfig {

    // ------- 已注册的实现类列表 ------- //
    private static List<Function> functions;
    private static List<FastJson2Interceptor> fastJson2Interceptors;
    private static List<LocalDataLoader> fileDataLoaders;
    private static List<AllureFilter> allureFilters;

    private static List<SessionRunnerInheritance> sessionRunnerInheritances;
    private static List<GrootListener> grootListeners;
    private static List<TestRunnerListener> testRunnerListeners;
    private static List<SessionRunnerListener> sessionRunnerListeners;

    // ------- 关键字字典 ------- //
    // JSON Key 与对应类的关联关系，用于解析 JSON / Yaml 用例
    // key:   所有 TestElement 中全局唯一的 reference key，如："if"、"testcase"
    // value：TestElement 实现类，如：IfController.class
    private static Map<String, Class<? extends TestElement>> testElementKeyMap;
    private static Map<String, Class<? extends ConfigItem>> configItemKeyMap;
    private static Map<String, Class<? extends PreProcessor>> preProcessorKeyMap;
    private static Map<String, Class<? extends Extractor>> extractorKeyMap;
    private static Map<String, Class<? extends Assertion>> assertionKeyMap;
    private static Map<String, Class<? extends PostProcessor>> postProcessorKeyMap;
    // 特别提醒：AllureFilter 等非用户侧 Filter（用不到关键字），不要注册进来
    private static Map<String, Class<? extends TestFilter>> testFilterKeyMap;
    private static Map<String, Class<? extends MappingFunction>> mappingKeyMap;

    private static Map<String, Class<?>> assertionValueTypeMap;

    // ------- 数据更新读写锁 ------- //
    // 应用场景：动态加载 Jar 后更新关键字字典，保证线程读写安全

    // 列表相关读写锁
    private static final ReadWriteLock functionsLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock fastJson2InterceptorsLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock fileDataLoadersLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock allureFiltersLock = new ReentrantReadWriteLock(false);

    private static final ReadWriteLock sessionRunnerInheritancesLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock grootListenersLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock testRunnerListenersLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock sessionRunnerListenersLock = new ReentrantReadWriteLock(false);

    // 关键字字典相关读写锁
    private static final ReadWriteLock testElementKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock configItemKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock preProcessorKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock extractorKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock assertionKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock postProcessorKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock testFilterKeyMapLock = new ReentrantReadWriteLock(false);
    private static final ReadWriteLock mappingKeyMapLock = new ReentrantReadWriteLock(false);

    /**
     * 已注册的 Function 列表
     *
     * @return 所有已注册函数
     */
    public static List<Function> getFunctions() {
        return readData(functionsLock,
            () -> ApplicationConfig.functions,
            () -> {
                ApplicationConfig.functions = GrootServiceLoader.loadAsListBySPI(Function.class);
                return ApplicationConfig.functions;
            });
    }

    /**
     * 设置或更新函数列表
     *
     * @param functions 函数列表
     */
    public static void setFunctions(List<Function> functions) {
        writeData(functionsLock,
            () -> ApplicationConfig.functions = Collections.unmodifiableList(functions));
    }

    /**
     * 已注册的 FastJson2 拦截器列表
     *
     * @return 所有已注册 FastJson2 拦截器
     */
    public static List<FastJson2Interceptor> getFastJson2Interceptors() {
        return readData(fastJson2InterceptorsLock,
            () -> ApplicationConfig.fastJson2Interceptors,
            () -> {
                ApplicationConfig.fastJson2Interceptors = GrootServiceLoader.loadAsListBySPI(FastJson2Interceptor.class);
                return ApplicationConfig.fastJson2Interceptors;
            });
    }

    /**
     * 设置或更新 FastJson2 拦截器列表
     *
     * @param fastJson2Interceptors FastJson2 拦截器列表
     */
    public static void setFastJson2Interceptors(List<FastJson2Interceptor> fastJson2Interceptors) {
        writeData(fastJson2InterceptorsLock,
            () -> ApplicationConfig.fastJson2Interceptors = Collections.unmodifiableList(fastJson2Interceptors));
    }

    /**
     * 已注册的 FileDataLoader 列表
     *
     * @return 所有已注册 FileDataLoader
     */
    public static List<LocalDataLoader> getFileDataLoaders() {
        return readData(fileDataLoadersLock,
            () -> ApplicationConfig.fileDataLoaders,
            () -> {
                ApplicationConfig.fileDataLoaders = GrootServiceLoader.loadAsListBySPI(LocalDataLoader.class);
                return ApplicationConfig.fileDataLoaders;
            });
    }

    /**
     * 设置或更新 FileDataLoader 列表
     *
     * @param fileDataLoaders FileDataLoader 列表
     */
    public static void setFileDataLoaders(List<LocalDataLoader> fileDataLoaders) {
        writeData(fileDataLoadersLock,
            () -> ApplicationConfig.fileDataLoaders = Collections.unmodifiableList(fileDataLoaders));
    }

    /**
     * 已注册的 AllureListener 列表
     *
     * @return 所有已注册 AllureListener
     */
    public static List<AllureFilter> getAllureFilters() {
        return readData(allureFiltersLock,
            () -> ApplicationConfig.allureFilters,
            () -> {
                ApplicationConfig.allureFilters = GrootServiceLoader.loadAsListBySPI(AllureFilter.class);
                return ApplicationConfig.allureFilters;
            });
    }

    /**
     * 设置或更新 AllureListener 列表
     *
     * @param allureFilters AllureListener 列表
     */
    public static void setAllureFilters(List<AllureFilter> allureFilters) {
        writeData(allureFiltersLock,
            () -> ApplicationConfig.allureFilters = Collections.unmodifiableList(allureFilters));
    }

    public static List<SessionRunnerInheritance> getSessionRunnerInheritances() {
        return readData(sessionRunnerInheritancesLock,
            () -> ApplicationConfig.sessionRunnerInheritances,
            () -> {
                ApplicationConfig.sessionRunnerInheritances = GrootServiceLoader.loadAsListBySPI(SessionRunnerInheritance.class);
                return ApplicationConfig.sessionRunnerInheritances;
            });
    }

    public static void setSessionRunnerInheritances(List<SessionRunnerInheritance> sessionRunnerInheritances) {
        writeData(sessionRunnerInheritancesLock,
            () -> ApplicationConfig.sessionRunnerInheritances = Collections.unmodifiableList(sessionRunnerInheritances));
    }

    public static List<GrootListener> getGrootListeners() {
        return readData(grootListenersLock,
            () -> ApplicationConfig.grootListeners,
            () -> {
                ApplicationConfig.grootListeners = GrootServiceLoader.loadAsListBySPI(GrootListener.class);
                return ApplicationConfig.grootListeners;
            });
    }

    public static void setGrootListeners(List<GrootListener> grootListeners) {
        writeData(grootListenersLock,
            () -> ApplicationConfig.grootListeners = Collections.unmodifiableList(grootListeners));
    }

    public static List<TestRunnerListener> getTestRunnerListeners() {
        return readData(testRunnerListenersLock,
            () -> ApplicationConfig.testRunnerListeners,
            () -> {
                ApplicationConfig.testRunnerListeners = GrootServiceLoader.loadAsListBySPI(TestRunnerListener.class);
                return ApplicationConfig.testRunnerListeners;
            });
    }

    public static void setTestRunnerListeners(List<TestRunnerListener> testRunnerListeners) {
        writeData(testRunnerListenersLock,
            () -> ApplicationConfig.testRunnerListeners = Collections.unmodifiableList(testRunnerListeners));
    }

    public static List<SessionRunnerListener> getSessionRunnerListeners() {
        return readData(sessionRunnerListenersLock,
            () -> ApplicationConfig.sessionRunnerListeners,
            () -> {
                ApplicationConfig.sessionRunnerListeners = GrootServiceLoader.loadAsListBySPI(SessionRunnerListener.class);
                return ApplicationConfig.sessionRunnerListeners;
            });
    }

    public static void setSessionRunnerListeners(List<SessionRunnerListener> sessionRunnerListeners) {
        writeData(sessionRunnerListenersLock,
            () -> ApplicationConfig.sessionRunnerListeners = Collections.unmodifiableList(sessionRunnerListeners));
    }


    /**
     * TestElement 关键字字典。
     * <p>关键字示例： <code>if <=> IfController.class</code>
     * <p>查找顺序
     * <ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends TestElement>> getTestElementKeyMap() {
        return readData(testElementKeyMapLock,
            () -> ApplicationConfig.testElementKeyMap,
            () -> {
                ApplicationConfig.testElementKeyMap = GrootServiceLoader.loadAsMapBySPI(TestElement.class);
                return ApplicationConfig.testElementKeyMap;
            });

        // 等效以下代码
        //Lock readLock = testElementKeyMapLock.readLock();
        //Lock writeLock = testElementKeyMapLock.writeLock();
        //readLock.lock();
        //try {
        //    if (testElementKeyMap != null) {
        //        return testElementKeyMap;
        //    }
        //} finally {
        //    readLock.unlock();
        //}
        //
        //writeLock.lock();
        //try {
        //    // 此时可能有多个线程通过了前面的代码并将持有写锁，如果不判断 Null，会导致多次加载
        //    // 未通过 API 注册，或之前未调用过该方法
        //    if (testElementKeyMap == null) {
        //        // SPI 查找并注册
        //        testElementKeyMap = GrootServiceLoader.loadAsMapBySPI(TestElement.class);
        //    }
        //    return testElementKeyMap;
        //} finally {
        //    writeLock.unlock();
        //}
    }

    /**
     * 设置或更新 TestElement 关键字字典
     *
     * @param testElementKeyMap 关键字字典
     */
    public static void setTestElementKeyMap(Map<String, Class<? extends TestElement>> testElementKeyMap) {
        writeData(testElementKeyMapLock, () -> {
            // 包装为不可变类型，保证 Map 只读，从而保证线程安全
            ApplicationConfig.testElementKeyMap = Collections.unmodifiableMap(testElementKeyMap);
        });

        // 等效以下代码
        //Lock writeLock = testElementKeyMapLock.writeLock();
        //writeLock.lock();
        //try {
        //    // 包装为不可变类型，保证 Map 只读，从而保证线程安全
        //    ApplicationConfig.testElementKeyMap = Collections.unmodifiableMap(testElementKeyMap);
        //} finally {
        //    writeLock.unlock();
        //}
    }

    /**
     * ConfigItem 关键字字典
     * <p>关键字示例： <code>http <=> HttpConfig.class</code>
     * <p>查找顺序<ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends ConfigItem>> getConfigItemKeyMap() {
        return readData(configItemKeyMapLock,
            () -> ApplicationConfig.configItemKeyMap,
            () -> {
                ApplicationConfig.configItemKeyMap = GrootServiceLoader.loadAsMapBySPI(ConfigItem.class);
                return ApplicationConfig.configItemKeyMap;
            });
    }

    /**
     * 设置或更新 ConfigItem 关键字字典
     *
     * @param configItemKeyMap 关键字字典
     */
    public static void setConfigItemKeyMap(Map<String, Class<? extends ConfigItem>> configItemKeyMap) {
        writeData(configItemKeyMapLock,
            () -> ApplicationConfig.configItemKeyMap = Collections.unmodifiableMap(configItemKeyMap));
    }

    /**
     * PreProcessor 关键字字典。
     * <p>关键字示例： <code>hooks <=> HooksPreProcessor.class</code>
     * <p>查找顺序<ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends PreProcessor>> getPreProcessorKeyMap() {
        return readData(preProcessorKeyMapLock,
            () -> ApplicationConfig.preProcessorKeyMap,
            () -> {
                ApplicationConfig.preProcessorKeyMap = GrootServiceLoader.loadAsMapBySPI(PreProcessor.class);
                return ApplicationConfig.preProcessorKeyMap;
            });
    }

    /**
     * 设置或更新 PreProcessor 关键字字典
     *
     * @param preProcessorKeyMap 关键字字典
     */
    public static void setPreProcessorKeyMap(Map<String, Class<? extends PreProcessor>> preProcessorKeyMap) {
        writeData(preProcessorKeyMapLock,
            () -> ApplicationConfig.preProcessorKeyMap = Collections.unmodifiableMap(preProcessorKeyMap));
    }

    /**
     * Extractor 关键字字典。
     * <p>关键字示例： <code>jsonpath <=> JsonPathExtractor.class</code>
     * <p>查找顺序<ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends Extractor>> getExtractorKeyMap() {
        return readData(extractorKeyMapLock,
            () -> ApplicationConfig.extractorKeyMap,
            () -> {
                ApplicationConfig.extractorKeyMap = GrootServiceLoader.loadAsMapBySPI(Extractor.class);
                return ApplicationConfig.extractorKeyMap;
            });
    }

    /**
     * 设置或更新 Extractor 关键字字典
     *
     * @param extractorKeyMap 关键字字典
     */
    public static void setExtractorKeyMap(Map<String, Class<? extends Extractor>> extractorKeyMap) {
        writeData(extractorKeyMapLock,
            () -> ApplicationConfig.extractorKeyMap = Collections.unmodifiableMap(extractorKeyMap));
    }

    /**
     * Assertion 关键字字典。
     * <p>关键字示例： <code>equalTo <=> EqualToAssertion.class</code>
     * <p>查找顺序<ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends Assertion>> getAssertionKeyMap() {
        return readData(assertionKeyMapLock,
            () -> ApplicationConfig.assertionKeyMap,
            () -> {
                ApplicationConfig.assertionKeyMap = GrootServiceLoader.loadAsMapBySPI(Assertion.class);
                parseMatcherValueTypeFromAssertionKeyMap();
                return ApplicationConfig.assertionKeyMap;
            });
    }

    public static Map<String, Class<?>> getAssertionValueTypeMap() {
        return assertionValueTypeMap;
    }

    /**
     * 设置或更新 Assertion 关键字字典
     *
     * @param assertionKeyMap 关键字字典
     */
    public static void setAssertionKeyMap(Map<String, Class<? extends Assertion>> assertionKeyMap) {
        writeData(assertionKeyMapLock,
            () -> {
                ApplicationConfig.assertionKeyMap = Collections.unmodifiableMap(assertionKeyMap);
                parseMatcherValueTypeFromAssertionKeyMap();
            });
    }

    // 解析 MatcherAssertion 的值类型信息
    private static void parseMatcherValueTypeFromAssertionKeyMap() {
        ApplicationConfig.assertionValueTypeMap = new HashMap<>();
        for (Map.Entry<String, Class<? extends Assertion>> entry : assertionKeyMap.entrySet()) {
            Class<? extends Assertion> clazz = entry.getValue();
            if (MatcherAssertion.class.isAssignableFrom(clazz)) {
                String key = entry.getKey();
                MatcherValueType valueType = clazz.getAnnotation(MatcherValueType.class);
                if (valueType != null) {
                    assertionValueTypeMap.put(key, valueType.value());
                }
            }
        }
    }

    /**
     * PostProcessor 关键字字典。
     * <p>关键字示例： <code>hooks <=> HooksPostProcessor.class</code>
     * <p>查找顺序<ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends PostProcessor>> getPostProcessorKeyMap() {
        return readData(postProcessorKeyMapLock,
            () -> ApplicationConfig.postProcessorKeyMap,
            () -> {
                ApplicationConfig.postProcessorKeyMap = GrootServiceLoader.loadAsMapBySPI(PostProcessor.class);
                return ApplicationConfig.postProcessorKeyMap;
            });
    }

    /**
     * 设置或更新 PostProcessor 关键字字典
     *
     * @param postProcessorKeyMap 关键字字典
     */
    public static void setPostProcessorKeyMap(Map<String, Class<? extends PostProcessor>> postProcessorKeyMap) {
        writeData(postProcessorKeyMapLock,
            () -> ApplicationConfig.postProcessorKeyMap = Collections.unmodifiableMap(postProcessorKeyMap));
    }

    /**
     * TestElementListener 关键字字典。
     * <p>关键字示例： <code>my_encrypt_decrypt <=> MyEncryptDecrypt.class</code>
     * <p>查找顺序<ul>
     * <li>如果已通过 API 赋值则直接返回</li>
     * <li>否则通过 SPI 加载后赋值并返回</li>
     * </ul>
     *
     * @return JSON Key 与 Java 类型的关联关系
     */
    public static Map<String, Class<? extends TestFilter>> getTestFilterKeyMap() {
        return readData(testFilterKeyMapLock,
            () -> ApplicationConfig.testFilterKeyMap,
            () -> {
                ApplicationConfig.testFilterKeyMap = GrootServiceLoader.loadAsMapBySPI(TestFilter.class);
                return ApplicationConfig.testFilterKeyMap;
            });
    }

    /**
     * 设置或更新 TestElementListener 关键字字典
     *
     * @param testFilterKeyMap 关键字字典
     */
    public static void setTestFilterKeyMap(Map<String, Class<? extends TestFilter>> testFilterKeyMap) {
        writeData(testFilterKeyMapLock,
            () -> ApplicationConfig.testFilterKeyMap = Collections.unmodifiableMap(testFilterKeyMap));
    }

    public static Map<String, Class<? extends MappingFunction>> getMappingKeyMap() {
        return readData(mappingKeyMapLock,
            () -> ApplicationConfig.mappingKeyMap,
            () -> {
                ApplicationConfig.mappingKeyMap = GrootServiceLoader.loadAsMapBySPI(MappingFunction.class);
                return ApplicationConfig.mappingKeyMap;
            });
    }

    public static void setMappingKeyMap(Map<String, Class<? extends MappingFunction>> mappingKeyMap) {
        writeData(mappingKeyMapLock,
            () -> ApplicationConfig.mappingKeyMap = Collections.unmodifiableMap(mappingKeyMap));
    }


    private static <T> T readData(ReadWriteLock readWriteLock,
                                  Supplier<T> readWorker,
                                  Supplier<T> writeWorker) {
        // readWorker 和 writeWorker lambda 表达式中没有使用上下文变量（外部变量），因此 lambda 不会每次调用都创建新对象
        Lock readLock = readWriteLock.readLock();
        Lock writeLock = readWriteLock.writeLock();
        readLock.lock();
        try {
            T t = readWorker.get();
            if (t != null) {
                return t;
            }
        } finally {
            readLock.unlock();
        }

        writeLock.lock();
        try {
            T t = readWorker.get();
            // 此时可能有多个线程通过了前面的代码并将持有写锁，如果不判断 Null，会导致多次加载
            if (t == null) {
                t = writeWorker.get();
            }
            return t;
        } finally {
            writeLock.unlock();
        }
    }

    private static void writeData(ReadWriteLock readWriteLock, Worker writeWorker) {
        // writeWorker lambda 表达式中使用了上下文变量（外部变量），因此每次调用 writeWorker 都是新对象，
        // 但更新方法调用频率很低，不影响
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            writeWorker.work();
        } finally {
            writeLock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public static ApplicationData getApplicationData() {
        return ApplicationData.SINGLETON;
    }

    public static String getWorkDirectory() {
        return ApplicationData.SINGLETON.getWorkDirectory();
    }

}
