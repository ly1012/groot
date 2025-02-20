package com.liyunx.groot.config;

import java.util.HashMap;

/**
 * 测试元件配置上下文
 */
@SuppressWarnings({"rawtypes"})
public class TestElementConfig extends HashMap<String, ConfigItem> implements ConfigGroup {

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends ConfigItem<T>> T get(String key) {
        return (T) super.get(key);
    }

    @Override
    public TestElementConfig copy() {
        TestElementConfig testElementConfig = new TestElementConfig();
        this.forEach((k, v) -> testElementConfig.put(k, v.copy()));
        return testElementConfig;
    }

}
