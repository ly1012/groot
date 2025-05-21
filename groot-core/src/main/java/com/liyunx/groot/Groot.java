package com.liyunx.groot;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.EnvironmentConfig;
import com.liyunx.groot.config.GlobalConfig;
import com.liyunx.groot.config.builtin.FilterConfigItem;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.EnvironmentContext;
import com.liyunx.groot.context.GlobalContext;
import com.liyunx.groot.dataloader.fastjson2.deserializer.MatcherObjectReader;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.model.ApplicationEnvironment;
import com.liyunx.groot.util.CollectionUtil;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * 测试执行入口。
 *
 * <p>一般情况下一次批运行（如 testng.xml）实例化一个 Groot 对象，每次实例化会重新加载全局配置和环境配置，Groot 对象可在多线程间共享。
 *
 * <p>default 为环境名称保留字，表示未指定环境时的默认环境，为一个空的环境对象。用户自定义环境请使用其他名称。
 */
public class Groot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Groot.class);

    public static final boolean FieldBased = false;

    private final Configuration configuration;
    private final GlobalContext globalContext;
    private final EnvironmentContext environmentContext;
    private final String environmentName;

    private volatile boolean isStarted = false;
    private volatile boolean isEnded = false;

    // TODO 是否需要提供运行时环境切换的能力？如果用户不注意可能会有不少坑，风险较大
    private final Map<String, EnvironmentContext> cachedEnvironments = new HashMap<>();

    static {
        JSON.register(Matcher.class, MatcherObjectReader.singleInstance);

        // TODO 应该取消全局配置，方法调用时单独设置？
        // FastJson 是静态调用，Jackson 是对象调用，因此一个用户修改 FastJson 全局配置后，会影响其他用户。
        //JSON.config(
        //    JSONReader.Feature.FieldBased,
        //    JSONReader.Feature.UseNativeObject
        //);
    }

    /**
     * 使用默认配置：
     * 本地文件形式 + 默认环境（default：一个空的环境）
     */
    public Groot() {
        this(null);
    }

    /**
     * 使用默认配置：
     * 本地文件形式 + 指定环境
     *
     * @param environmentName 环境名称
     */
    public Groot(String environmentName) {
        this(Configuration.generateDefaultConfiguration(), environmentName);
    }

    /**
     * 使用指定配置 + 指定环境
     *
     * @param configuration   自定义运行配置项
     * @param environmentName 指定的环境名称，表示基于该环境运行。
     *                        如果未指定（null）尝试读取系统属性 groot.environment.name 的值。
     *                        如果系统属性 env 未设置，则使用默认环境 default，即一个空的环境。
     */
    public Groot(Configuration configuration, String environmentName) {
        // 检查 Configuration
        checkConfiguration(configuration);
        this.configuration = configuration;

        // 加载全局数据
        globalContext = loadGlobalContext();

        // 加载环境数据
        this.environmentName = parseEnvironmentName(environmentName);
        environmentContext = loadEnvironmentContext();
    }

    private void checkConfiguration(Configuration configuration) {
        if (configuration == null)
            throw new IllegalArgumentException("Configuration 不能为 null");

        ValidateResult validateResult = configuration.validate();
        if (!validateResult.isValid())
            throw new InvalidDataException(validateResult.getReason());
    }

    private GlobalContext loadGlobalContext() {
        // 创建全局上下文
        GlobalContext globalContext = new GlobalContext();

        // 加载全局配置，并设置默认值
        GlobalConfig globalConfig = configuration.getGlobalConfigLoader().load();
        if (globalConfig == null) {
            LOGGER.warn("全局配置未指定，使用默认配置");
            globalConfig = new GlobalConfig();
        }
        if (globalConfig.getVariableConfigItem() == null)
            globalConfig.put(VariableConfigItem.KEY, new VariableConfigItem());
        if (globalConfig.get(FilterConfigItem.KEY) == null)
            globalConfig.put(FilterConfigItem.KEY, new FilterConfigItem());

        // 注册内置 Filter（TestFilter）
        FilterConfigItem filterConfigItem = globalConfig.get(FilterConfigItem.KEY);
        List<TestFilter> builtinTestFilters = configuration.getBuiltinTestFilters();
        if (builtinTestFilters != null) {
            filterConfigItem.addAll(builtinTestFilters);
        }

        ValidateResult validateResult = globalConfig.validate();
        if (!validateResult.isValid()) {
            throw new InvalidDataException("全局配置校验失败：%s", validateResult.getReason());
        }

        // 替换全局变量中的表达式
        configuration
            .getTemplateEngine()
            .eval(null, globalConfig.getVariableConfigItem());

        globalContext.setConfigGroup(globalConfig);
        return globalContext;
    }

    // 计算环境名称
    private String parseEnvironmentName(String environmentName) {
        if (environmentName != null && !environmentName.trim().isEmpty())
            return environmentName;

        // 读取优先级（优先级低的在前）：
        // 1. groot-test.yml 等应用配置文件
        // 2. 命令行参数 -Dgroot.environment.active=<environmentName>

        // {@link ApplicationData}
        // 如果指定名称和配置名称都为空，则使用默认环境名称 default
        String active = ApplicationConfig.getApplicationData().getEnvironment().getActive();
        if (isNull(active) || active.trim().isEmpty()) {
            active = ApplicationEnvironment.DEFAULT_ENV_NAME;
        }
        return active;
    }

    private EnvironmentContext loadEnvironmentContext() {
        LOGGER.info("加载环境 {}", environmentName);
        EnvironmentContext environmentContext = new EnvironmentContext();

        // 未指定环境时，默认使用一个空环境
        if (ApplicationEnvironment.DEFAULT_ENV_NAME.equals(environmentName)) {
            environmentContext.setConfigGroup(new EnvironmentConfig());
            return environmentContext;
        }

        // 加载指定环境的数据
        EnvironmentConfig environmentConfig = configuration.getEnvironmentLoader().load(environmentName);
        if (environmentConfig == null) {
            LOGGER.warn("未找到环境 {}，使用默认环境 default", environmentName);
            environmentConfig = new EnvironmentConfig();
        }
        if (environmentConfig.getVariableConfigItem() == null)
            environmentConfig.put(VariableConfigItem.KEY, new VariableConfigItem());

        ValidateResult validateResult = environmentConfig.validate();
        if (!validateResult.isValid()) {
            throw new InvalidDataException("环境配置校验失败：%s", validateResult.getReason());
        }

        // 替换环境变量中的表达式
        configuration
            .getTemplateEngine()
            .eval(new ContextWrapper(CollectionUtil.listOf(globalContext)), environmentConfig.getVariableConfigItem());

        environmentContext.setConfigGroup(environmentConfig);
        return environmentContext;
    }

    public TestRunner newTestRunner() {
        return new TestRunner(this);
    }

    /**
     * 执行初始化动作
     */
    public void start() {
        if (isStarted) {
            throw new IllegalStateException("Groot 已经启动");
        }
        synchronized (this) {
            if (isStarted) {
                throw new IllegalStateException("Groot 已经启动");
            }
            ApplicationConfig.getGrootListeners().forEach(listener -> listener.grootStart(this));
            isStarted = true;
        }
    }

    /**
     * 执行清理动作
     */
    public void stop() {
        if (isEnded) {
            throw new IllegalStateException("Groot 已经停止");
        }
        synchronized (this) {
            if (isEnded) {
                throw new IllegalStateException("Groot 已经停止");
            }
            ApplicationConfig.getGrootListeners().forEach(listener -> listener.grootStop(this));
            isEnded = true;
        }
    }

    // ---------------------------------------------------------------------
    // Getter
    // ---------------------------------------------------------------------

    public Configuration getConfiguration() {
        return configuration;
    }

    public GlobalContext getGlobalContext() {
        return globalContext;
    }

    public EnvironmentContext getEnvironmentContext() {
        return environmentContext;
    }

    public String getEnvironmentName() {
        return environmentName;
    }
}
