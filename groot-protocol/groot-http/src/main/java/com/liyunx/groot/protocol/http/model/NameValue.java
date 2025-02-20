package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 键值对
 */
public class NameValue<T> {

    /**
     * 是否已编码
     */
    @JSONField(name = "encoded")
    protected boolean encoded;

    @JSONField(name = "name")
    protected String name;      //参数名称

    @JSONField(name = "value")
    protected T value;          //参数值

    public boolean isEncoded() {
        return encoded;
    }

    public void setEncoded(boolean encoded) {
        this.encoded = encoded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}
