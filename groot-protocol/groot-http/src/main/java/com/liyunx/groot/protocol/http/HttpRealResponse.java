package com.liyunx.groot.protocol.http;

import com.liyunx.groot.protocol.http.model.FormParamManager;
import com.liyunx.groot.protocol.http.model.HeaderManager;
import com.liyunx.groot.protocol.http.model.MultiPart;
import com.liyunx.groot.testelement.RealResponse;

/**
 * Http 实际响应数据
 */
public class HttpRealResponse extends RealResponse {

    // == 状态行 ==

    /**
     * 协议与版本信息，如 HTTP/1.1
     */
    private String protocol;

    /**
     * 响应状态码，如 200 404
     */
    private int status;

    /**
     * 响应状态描述信息，如 OK，可能为空
     */
    private String message;

    // == 响应头 ==

    /**
     * 响应头
     */
    private HeaderManager headers;

    /** 响应头（单列）：Set-Cookie 信息 */
    //private Map<String, String> cookies;

    /** 响应头（单列）：Set-Cookie 信息（CookieJar？） */
    //private Map<String, String> cookieJars;

    // == 响应体 ==

    // extends from RealResponse

    /**
     * 响应体（单列）：application/x-www-form-urlencoded 类型
     */
    private FormParamManager form;

    /**
     * 响应体（单列）：multipart 类型
     */
    private MultiPart multipart;

    // == Getter/Setter ==

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HeaderManager getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderManager headers) {
        this.headers = headers;
    }

    public FormParamManager getForm() {
        return form;
    }

    public void setForm(FormParamManager form) {
        this.form = form;
    }

    public MultiPart getMultipart() {
        return multipart;
    }

    public void setMultipart(MultiPart multipart) {
        this.multipart = multipart;
    }
}
