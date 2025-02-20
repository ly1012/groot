package com.liyunx.groot.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 环境配置
 */
public class EnvironmentConfig extends ConcurrentHashMap<String, ConfigItem> implements ConfigGroup {

    @Override
    public <T extends ConfigItem<T>> T get(String key) {
        return (T) super.get(key);
    }

    @Override
    public EnvironmentConfig copy() {
        EnvironmentConfig environment = new EnvironmentConfig();
        this.forEach((k, v) -> environment.put(k, v.copy()));
        return environment;
    }

}
