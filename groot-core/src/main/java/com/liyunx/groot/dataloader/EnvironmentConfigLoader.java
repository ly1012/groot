package com.liyunx.groot.dataloader;

import com.liyunx.groot.config.EnvironmentConfig;

/**
 * 环境数据加载接口
 *
 * <pre><code>
 *     // 如果存在多个项目：
 *     Configuration configuration = new Configuration();
 *     configuration.setEnvironmentLoader(new PlatformEnvironmentLoader(projectId));
 *     ...
 *     Groot groot = new Groot(configuration, environmentName);
 * </code></pre>
 */
@FunctionalInterface
public interface EnvironmentConfigLoader {

    /**
     * 加载指定名称的环境
     *
     * @param environmentName 环境名称，参数不会为 null 或空，如果指定环境不存在应抛出异常
     * @return 环境数据
     */
    EnvironmentConfig load(String environmentName);

}
