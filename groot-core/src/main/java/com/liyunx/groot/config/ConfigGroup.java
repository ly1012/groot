package com.liyunx.groot.config;

import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.builtin.VariableConfigItem;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 配置组接口，一个配置组由多个不同类型的 ConfigItem 及其唯一 key 组成。
 *
 * <p><br>一个简单的配置组示例：
 * <blockquote><pre>
 *   http: HttpConfigItem
 *   variables: VariableConfigItem
 * </pre></blockquote>
 */
@SuppressWarnings("rawtypes")
public interface ConfigGroup extends Map<String, ConfigItem>, ConfigElement<ConfigGroup> {

    /**
     * 读取指定配置项。
     *
     * <p>{@link Map#get} 语法糖，方法内部会自动强制类型转换。
     * HttpConfig config = configGroup.get("http");
     *
     * @param key 配置项 Key，如 http
     * @param <T> 配置项对象类型，如 HttpConfig
     * @return 配置项对象
     */
    <T extends ConfigItem<T>> T get(String key);

    /**
     * 读取指定配置项。
     *
     * @param clazz 配置项类型，如 HttpConfig.class
     * @param <T>   配置项类型
     * @return 配置项对象
     */
    default <T extends ConfigItem<T>> T get(Class<T> clazz) {
        String key = ApplicationConfig.getConfigItemKeyMap().entrySet().stream()
            .filter(entry -> entry.getValue().equals(clazz))
            .findFirst()
            .get()
            .getKey();
        return get(key);
    }

    ConfigGroup copy();

    /**
     * 合并配置组，参数配置组的配置项覆盖当前对象配置组的配置项
     *
     * @param other 配置组
     * @return 返回合并后的新对象
     */
    default ConfigGroup merge(ConfigGroup other) {
        // 创建新的空对象
        ConfigGroup res = this.copy();

        // 参数为 null
        if (other == null) {
            return res;
        }

        // 合并所有存在的 Key
        Set<String> keys = new HashSet<>(this.keySet());
        keys.addAll(other.keySet());

        // 合并值，参数的值覆盖当前对象的值
        for (String key : keys) {
            @SuppressWarnings("unchecked")
            ConfigItem oldItem = this.get(key);
            @SuppressWarnings("unchecked")
            ConfigItem newItem = other.get(key);
            if (oldItem != null && newItem != null) {
                @SuppressWarnings("unchecked")
                ConfigItem item = oldItem.merge(newItem);
                res.put(key, item);
            } else if (newItem != null) {
                res.put(key, newItem.copy());
            }
        }

        return res;
    }

    @Override
    default ValidateResult validate() {
        ValidateResult r = new ValidateResult();
        this.values().forEach(r::append);
        return r;
    }

    default VariableConfigItem getVariableConfigItem() {
        return get(VariableConfigItem.KEY);
    }

}
