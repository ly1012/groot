package com.liyunx.groot.dataloader;

import com.liyunx.groot.config.GlobalConfig;

/**
 * 全局数据加载接口
 */
@FunctionalInterface
public interface GlobalConfigLoader {

    /**
     * 加载全局数据
     *
     * @return 全局配置数据
     */
    GlobalConfig load();

}
