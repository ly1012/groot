package com.liyunx.groot.model;

import com.alibaba.fastjson2.util.PropertiesUtils;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.dataloader.file.FileType;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.util.YamlUtil;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * 应用配置数据：JVM 内共享的配置或 Groot 实例的默认配置
 * <p>
 * 应用加载时确定（第一个用例执行前可以修改），加载完成后不应该再修改。
 * <p>
 * 应用配置文件支持本地文件路径或 Jar 中资源路径。Groot 实例配置文件（即 global.yml 和 env-test.yml）和数据文件仅支持本地文件路径。
 */
public class ApplicationData {

    private static final Logger log = LoggerFactory.getLogger(ApplicationData.class);

    // 命令行参数 -Dgroot.config.location，指定 groot 应用配置文件路径
    public static final String GROOT_CONFIG_LOCATION = "groot.config.location";

    // 单例，类加载时赋值
    public static final ApplicationData NONE = new ApplicationData();
    public static final ApplicationData SINGLETON;

    static {
        ApplicationData data = readApplicationData();
        if (data == null) {
            data = new ApplicationData();
        }
        SINGLETON = data;
        // 命令行参数 -D 具有最高优先级
        loadFromSystemProperties(data);
    }

    private static ApplicationData readApplicationData() {
        // 读取优先级：
        // src/test/resources/groot-test.yml
        // src/test/resources/groot-test.yaml
        // src/test/resources/groot-test.properties
        // src/main/resources/groot.yml
        // src/main/resources/groot.yaml
        // src/main/resources/groot.properties
        String[] applicationFileNames = new String[]{
            "groot-test.yml", "groot-test.yaml", "groot-test.properties",
            "groot.yml", "groot.yaml", "groot.properties"};
        ApplicationData data = null;

        // 先尝试读取指定文件
        // 使用场景：命令行运行
        String configLocation = System.getProperty(GROOT_CONFIG_LOCATION);
        if (nonNull(configLocation) && !configLocation.trim().isEmpty()) {
            data = loadFileAsApplicationData(configLocation);
            if (data != NONE) {
                return data;
            }
        }

        // 读取默认位置本地文件（当前工作目录 groot[-test].{yml, yaml, properties}）
        // 使用场景：命令行运行
        for(String applicationFileName : applicationFileNames) {
            data = loadFileAsApplicationData(applicationFileName);
            if (data != NONE) {
                return data;
            }
        }

        // 读取默认位置文件（类路径，target/classes 或 target/test-classes）
        // 使用场景：Maven / IDE 运行
        ClassLoader classLoader = ApplicationConfig.class.getClassLoader();
        if (classLoader == null) {
            return null;
        }
        for (int i = 0; i < applicationFileNames.length; i++) {
            try (InputStream inputStream = classLoader.getResourceAsStream(applicationFileNames[i])) {
                if (inputStream != null) {
                    if (i == 2 || i == 5) {
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        data = PropertiesUtils.toJavaObject(properties, ApplicationData.class);
                    } else {
                        data = YamlUtil.getYaml().loadAs(inputStream, ApplicationData.class);
                    }
                    if (data != null) {
                        return data;
                    }
                }
            } catch (IOException e) {
                throw new GrootException(e);
            }
        }

        return data;
    }

    private static ApplicationData loadFileAsApplicationData(String fileName) {
        if (isNull(fileName) || fileName.trim().isEmpty()) {
            return NONE;
        }

        fileName = fileName.trim();
        File file = Paths.get(fileName).toFile();
        if (!(file.exists() && file.isFile())) {
            return NONE;
        }

        String extensionName = FileUtil.getExtension(fileName);
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            if (FileType.isYamlFile(extensionName)) {
                return YamlUtil.getYaml().loadAs(inputStream, ApplicationData.class);
            } else if (FileType.isPropertiesFile(extensionName)) {
                Properties properties = new Properties();
                properties.load(inputStream);
                return PropertiesUtils.toJavaObject(properties, ApplicationData.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return NONE;
    }

    private static void loadFromSystemProperties(ApplicationData data) {
        PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
        System.getProperties().forEach((k, v) -> {
            // example: -Dgroot.environment.active=test
            if (k instanceof String ks && ks.length() > 6 && ks.startsWith("groot.")) {
                String propertyPath = ks.substring(6);
                try {
                    propertyUtilsBean.setNestedProperty(data, propertyPath, v);
                } catch (Exception e) {
                    log.warn("无法设置属性：{}，原因：{}", propertyPath, e.getMessage());
                }
            }
        });
    }

    /* ------------------------------------------------------------ */
    // 应用配置（JVM 内共享）

    // 默认工作目录
    private String workDirectory = "src/test/resources";

    /* ------------------------------------------------------------ */
    // Groot 实例默认配置

    private ApplicationEnvironment environment = new ApplicationEnvironment();

    // Allure 是否启用
    private boolean allure = false;

    public String getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
    }

    public ApplicationEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(ApplicationEnvironment environment) {
        this.environment = environment;
    }

    public boolean getAllure() {
        return allure;
    }

    public void setAllure(boolean allure) {
        this.allure = allure;
    }

}
