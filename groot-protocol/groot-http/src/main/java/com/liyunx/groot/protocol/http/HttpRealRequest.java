package com.liyunx.groot.protocol.http;

import com.liyunx.groot.protocol.http.model.HeaderManager;
import com.liyunx.groot.protocol.http.model.QueryParamManager;
import com.liyunx.groot.testelement.RealRequest;

import java.util.Map;

/**
 * Http 实际请求数据
 *
 * <p>注意和关键字属性 HttpRequest 的区别，HttpRequest 表示用例请求数据，它包括一些与请求内容无关的配置数据，
 * 而 HttpRealRequest 表示 Http 请求实际发送出去的数据，即抓包抓到的数据。</p>
 */
public class HttpRealRequest extends RealRequest {

    // == 请求行 ==

    /**
     * 请求方法，如 GET
     */
    private String method;

    /**
     * 请求 URL，如 https://testerhome.com/topics
     */
    private String url;

    /**
     * 请求 URL（单列）：请求查询参数
     */
    private QueryParamManager params;

    /**
     * 协议与版本，如 HTTP/1.1
     */
    private String protocol;

    // == 请求头 ==

    /**
     * 请求头
     */
    private HeaderManager headers;

    /**
     * 请求头（单列）：Cookie 信息
     */
    private Map<String, String> cookies;

    // == 请求体 ==

    // extends from RealRequest

    // == Getter/Setter ==

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

    public QueryParamManager getParams() {
        return params;
    }

    public void setParams(QueryParamManager params) {
        this.params = params;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public HeaderManager getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderManager headers) {
        this.headers = headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
