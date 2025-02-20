package com.liyunx.groot.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局配置
 */
@SuppressWarnings({"rawtypes"})
public class GlobalConfig extends ConcurrentHashMap<String, ConfigItem> implements ConfigGroup {

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends ConfigItem<T>> T get(String key) {
        return (T) super.get(key);
    }

    @Override
    public GlobalConfig copy() {
        GlobalConfig globalConfig = new GlobalConfig();
        this.forEach((k, v) -> globalConfig.put(k, v.copy()));
        return globalConfig;
    }

}
