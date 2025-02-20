package com.liyunx.groot.util;

import org.yaml.snakeyaml.Yaml;

/**
 * Yaml 工具类
 */
public class YamlUtil {

    private static final ThreadLocal<Yaml> yamlThreadLocal = ThreadLocal.withInitial(Yaml::new);

    public static Yaml getYaml() {
        return yamlThreadLocal.get();
    }

}
