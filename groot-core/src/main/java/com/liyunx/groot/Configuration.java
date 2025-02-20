package com.liyunx.groot;

import com.liyunx.groot.common.Validatable;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.dataloader.DataLoader;
import com.liyunx.groot.dataloader.EnvironmentConfigLoader;
import com.liyunx.groot.dataloader.GlobalConfigLoader;
import com.liyunx.groot.dataloader.file.EnvironmentConfigFileLoader;
import com.liyunx.groot.dataloader.file.GlobalConfigFileLoader;
import com.liyunx.groot.dataloader.file.LocalDataLoader;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.filter.allure.AllureFilter;
import com.liyunx.groot.model.ApplicationData;
import com.liyunx.groot.model.ApplicationEnvironment;
import com.liyunx.groot.template.TemplateEngine;
import com.liyunx.groot.template.freemarker.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 测试执行配置，每个 {@link Groot} 实例对应一个 {@link Configuration} 实例。
 */
public class Configuration implements Validatable {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private static Configuration defaultConfiguration;

    public static synchronized Configuration defaultConfiguration() {
        if (defaultConfiguration == null) {
            defaultConfiguration = generateDefaultConfiguration();
        }
        return defaultConfiguration;
    }

    public static Configuration generateDefaultConfiguration() {
        Configuration configuration = new Configuration();

        // 全局配置加载实现类
        configuration.setGlobalConfigLoader(new GlobalConfigFileLoader());

        // 环境加载实现类
        EnvironmentConfigFileLoader environmentConfigFileLoader = new EnvironmentConfigFileLoader();
        Optional.of(ApplicationConfig.getApplicationData())
            .map(ApplicationData::getEnvironment)
            .map(ApplicationEnvironment::getFilePrefix)
            .ifPresent(environmentConfigFileLoader::setEnvironmentFilePrefix);
        configuration.setEnvironmentLoader(environmentConfigFileLoader);

        // 数据加载实现类，默认本地文件加载
        List<LocalDataLoader> allFileDataLoader = ApplicationConfig.getFileDataLoaders();
        if (allFileDataLoader == null || allFileDataLoader.isEmpty()) {
            throw new InvalidDataException("未找到任何 DataLoader 实现类，默认 Configuration 生成失败！" +
                "\nDataLoader 用途：根据标识符进行数据加载，比如 Http 请求中引用请求模板时使用的标识符（文件相对路径或 ID 等）");
        }
        // 构建责任链
        LocalDataLoader prev = allFileDataLoader.get(0);
        for (int i = 1; i < allFileDataLoader.size(); i++) {
            LocalDataLoader current = allFileDataLoader.get(i);
            prev.setNext(current);
            prev = current;
        }
        configuration.setDataLoader(allFileDataLoader.get(0));

        // 模板引擎
        configuration.setTemplateEngine(new FreeMarkerTemplateEngine());

        // 内置 Filter（TestFilter）
        List<TestFilter> builtinFilters = new ArrayList<>();
        // 内置 Allure Filter
        if (isAllureEnabled()) {
            List<AllureFilter> allureFilters = ApplicationConfig.getAllureFilters();
            if (allureFilters != null) {
                builtinFilters.addAll(allureFilters);
            }
        }
        // 注册内置 Filter
        configuration.setBuiltinTestFilters(builtinFilters);

        return configuration;
    }

    private static boolean isAllureEnabled() {
        return ApplicationConfig.getApplicationData().getAllure();
    }

    // ----------- Configuration 配置 ------------- //

    private GlobalConfigLoader globalConfigLoader;
    private EnvironmentConfigLoader environmentLoader;
    private DataLoader dataLoader;
    private TemplateEngine templateEngine;
    private List<TestFilter> builtinTestFilters;

    @Override
    public ValidateResult validate() {
        ValidateResult res = new ValidateResult();
        if (globalConfigLoader == null) {
            res.append("\nGlobalConfigLoader 缺失，调用 Configuration#setGlobalConfigLoader 设置，GlobalConfigLoader 用于加载全局数据");
        }
        if (environmentLoader == null) {
            res.append("\nEnvironmentLoader 缺失，调用 Configuration#setEnvironmentLoader 设置，EnvironmentLoader 用于加载环境数据");
        }
        if (dataLoader == null) {
            res.append("\nDataLoader 缺失，调用 Configuration#setDataLoader 设置，DataLoader 用于加载测试数据（如用例数据等）");
        }
        if (templateEngine == null) {
            res.append("\nTemplateEngine 缺失，调用 Configuration#setTemplateEngine 设置，TemplateEngine 用于计算模板字符串，如 ${varName}");
        }
        return res;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public GlobalConfigLoader getGlobalConfigLoader() {
        return globalConfigLoader;
    }

    public void setGlobalConfigLoader(GlobalConfigLoader globalConfigLoader) {
        this.globalConfigLoader = globalConfigLoader;
    }

    public EnvironmentConfigLoader getEnvironmentLoader() {
        return environmentLoader;
    }

    public void setEnvironmentLoader(EnvironmentConfigLoader environmentLoader) {
        this.environmentLoader = environmentLoader;
    }

    public DataLoader getDataLoader() {
        return dataLoader;
    }

    public void setDataLoader(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public List<TestFilter> getBuiltinTestFilters() {
        return builtinTestFilters;
    }

    public void setBuiltinTestFilters(List<TestFilter> builtinTestFilters) {
        this.builtinTestFilters = builtinTestFilters;
    }
}
