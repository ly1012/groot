package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.common.Validatable;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;

import java.util.regex.Pattern;

/**
 * Http 代理实体类
 */
public class HttpProxy implements Copyable<HttpProxy>, Validatable, Computable<HttpProxy> {

    @JSONField(name = "ip")
    private String ip;

    @JSONField(name = "port")
    private String port;

    public HttpProxy() {
    }

    public HttpProxy(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    public HttpProxy(HttpProxy proxy) {
        this.ip = proxy.ip;
        this.port = proxy.port;
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();

        if (ip.split(Pattern.quote(".")).length != 4 && ip.split(":").length > 8) {
            r.append("\n配置项：proxy.ip，配置值：%s", ip);
        }

        //if (port < 0 || port > 65535) {
        //    r.append("\n端口号范围应该为 0 ~ 65535，当前端口号：%d", port);
        //}

        return r;
    }

    @Override
    public HttpProxy copy() {
        HttpProxy proxy = new HttpProxy();
        proxy.ip = ip;
        proxy.port = port;
        return proxy;
    }

    @Override
    public HttpProxy eval(ContextWrapper ctx) {
        ip = ctx.evalAsString(ip);
        port = ctx.evalAsString(port);
        return this;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

}
