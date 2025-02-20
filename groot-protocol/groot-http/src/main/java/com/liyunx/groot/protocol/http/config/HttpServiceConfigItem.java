package com.liyunx.groot.protocol.http.config;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.protocol.http.model.HeaderManager;
import com.liyunx.groot.protocol.http.model.HttpProxy;
import com.liyunx.groot.util.KryoUtil;

/**
 * 单个 Http 配置项（非一级配置项，不需要注册，故 ignore 设置为 true）
 */
@KeyWord(ignore = true)
public class HttpServiceConfigItem implements ConfigItem<HttpServiceConfigItem> {

    // == HTTP 请求内容字段 ==

    @JSONField(name = "base_url")
    private String baseUrl;
    @JSONField(name = "headers")
    private HeaderManager headers;

    // == HTTP 配置字段 ==

    /**
     * 设置代理服务器
     */
    @JSONField(name = "proxy")
    private HttpProxy proxy;

    /**
     * 是否进行 SSL 校验
     */
    @JSONField(name = "verify")
    private Boolean verify;

    /**
     * 原样发送 Body 数据，不解析表达式，比如请求 Body 为 10M 超大字符串或 JSON
     */
    @JSONField(name = "raw")
    private Boolean raw;

    // TODO more config field
    // protocol
    // followRedirects
    // followSslRedirects
    // dns

    // 如果需要支持表达式，需要使用 String 类型，而非 int 类型
    // callTimeout
    // connectTimeout
    // readTimeout
    // writeTimeout


    @Override
    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();

        //验证 base_url
        if (baseUrl != null && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            r.append("\n配置项：base_url，配置值：%s，必须以 http:// 或 https:// 开头", baseUrl);
        }

        //验证 headers
        //skip validate

        //验证 proxy
        r.append(proxy);

        return r;
    }

    @Override
    public HttpServiceConfigItem copy() {
        return KryoUtil.copy(this);

        //try {
        //  HttpConfigItem item = (HttpConfigItem) super.clone();
        //  if (headers != null){
        //    item.headers = (HashMap<String, String>) headers.clone();
        //  }
        //  if (proxy != null){
        //    item.proxy = proxy.clone();
        //  }
        //  return item;
        //} catch (CloneNotSupportedException e) {
        //  throw new GrootException(e);
        //}

        //HttpConfigItem item = new HttpConfigItem();
        //item.baseUrl = this.baseUrl;
        //item.verify = this.verify;
        //item.log = this.log;
        //if (headers != null){
        //  item.headers = new HashMap<>();
        //}
        //if (proxy != null){
        //  item.proxy = new HttpProxy();
        //}
        //return item;
    }

    @Override
    public HttpServiceConfigItem merge(HttpServiceConfigItem other) {
        HttpServiceConfigItem res = copy();

        if (null == other) return res;

        if (other.baseUrl != null) res.baseUrl = other.baseUrl;
        if (other.headers != null) {
            res.headers = res.headers == null ? other.headers.copy() : res.headers.merge(other.headers);
        }

        if (other.proxy != null) {
            res.proxy = other.proxy.copy();
        }
        if (other.verify != null) res.verify = other.verify;
        if (other.raw != null) res.raw = other.raw;

        return res;
    }

    // == Getter/Setter ==

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HeaderManager getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderManager headers) {
        this.headers = headers;
    }

    public HttpProxy getProxy() {
        return proxy;
    }

    public void setProxy(HttpProxy proxy) {
        this.proxy = proxy;
    }

    public Boolean getVerify() {
        return verify;
    }

    public void setVerify(Boolean verify) {
        this.verify = verify;
    }

    public Boolean getRaw() {
        return raw;
    }

    public void setRaw(Boolean raw) {
        this.raw = raw;
    }


    /**
     * {@link HttpServiceConfigItem} Builder
     */
    public static class Builder {

        // == HTTP 请求内容字段 ==

        private String baseUrl;
        private HeaderManager headers;

        // == HTTP 配置字段 ==

        /**
         * 设置代理服务器
         */
        private HttpProxy proxy;

        /**
         * 是否进行 SSL 校验
         */
        private Boolean verify;

        private Boolean log;

        /**
         * 原样发送 Body 数据，不解析表达式
         */
        private Boolean raw;

        public static Builder newBuilder() {
            return new HttpServiceConfigItem.Builder();
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder header(String headerName, String headerValue) {

            return this;
        }

        public Builder proxy(String ip, int port) {
            this.proxy = new HttpProxy(ip, port);
            return this;
        }

        public Builder proxy(HttpProxy httpProxy) {
            this.proxy = httpProxy;
            return this;
        }

        public Builder verify(boolean verify) {
            this.verify = verify;
            return this;
        }

        public HttpServiceConfigItem build() {
            HttpServiceConfigItem httpConfigItem = new HttpServiceConfigItem();

            httpConfigItem.setBaseUrl(baseUrl);
            httpConfigItem.setProxy(proxy);
            httpConfigItem.setVerify(verify);

            return httpConfigItem;
        }

    }
}
