package com.liyunx.groot.model;

import com.alibaba.fastjson2.util.PropertiesUtils;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.util.YamlUtil;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置数据
 */
public class ApplicationData {

    private static final Logger log = LoggerFactory.getLogger(ApplicationData.class);

    // 单例，初始化时赋值
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
        ClassLoader classLoader = ApplicationConfig.class.getClassLoader();
        if (classLoader == null) {
            return null;
        }

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

    // 默认工作目录
    private String workDirectory = "src/test/resources";

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
