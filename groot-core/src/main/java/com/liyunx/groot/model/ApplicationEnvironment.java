package com.liyunx.groot.model;

public class ApplicationEnvironment {

    // default 特别表示空环境，用户请使用其他环境名称
    public static final String DEFAULT_ENV_NAME = "default";

    // 默认执行环境
    private String active = DEFAULT_ENV_NAME;

    // 默认环境文件前缀为 env-
    // 前缀可以是 env/ 表示 env 目录，比如 env/test.yaml
    private String filePrefix = "env-";

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }
}
