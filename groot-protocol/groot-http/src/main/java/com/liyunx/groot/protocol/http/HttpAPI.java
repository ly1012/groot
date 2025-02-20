package com.liyunx.groot.protocol.http;


import com.alibaba.fastjson2.annotation.JSONField;

/**
 * HTTP 接口定义
 *
 * <p>该类作为 {@link HttpSampler} 的辅助存在，相当于数据文件。</p>
 */
public class HttpAPI {

    @JSONField(name = "service")
    private String serviceName;

    @JSONField(name = "method")
    private String method;

    @JSONField(name = "url")
    private String url;

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
