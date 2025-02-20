package com.liyunx.groot.config.builtin;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.ConfigItem;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * DNS Host 配置
 * <p>支持列表：
 * <ul>
 *     <li>HttpSampler（计划中）</li>
 * </ul>
 *
 */
@KeyWord(DnsConfigItem.KEY)
public class DnsConfigItem extends HashMap<String, String> implements ConfigItem<DnsConfigItem> {

    public static final String KEY = "dns";

    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();
        for (Entry<String, String> entry : this.entrySet()) {
            String hostname = entry.getKey();
            String ip = entry.getValue();
            // 简单验证
            if (hostname.split(Pattern.quote(".")).length < 2) {
                r.append("\n配置项：host-domain，配置值：%s", hostname);
            }
            if (ip.split(Pattern.quote(".")).length != 4 && ip.split(":").length > 8) {
                r.append("\n配置项：host-ip，配置值：%s", ip);
            }
        }
        return r;
    }

    @Override
    public DnsConfigItem merge(DnsConfigItem other) {
        DnsConfigItem hostConfigItem = new DnsConfigItem();
        hostConfigItem.putAll(this);
        hostConfigItem.putAll(other);
        return hostConfigItem;
    }

    @Override
    public DnsConfigItem copy() {
        return (DnsConfigItem) super.clone();
    }

}
