package com.liyunx.groot.protocol.http.constants;

/**
 * HTTP Header Field Name 常量
 */
public enum HttpHeader {

    COOKIE("Cookie"),
    CONTENT_TYPE("Content-Type"),
    CONTENT_DISPOSITION("Content-Disposition");

    private final String value;

    HttpHeader(String value) {
        this.value = value;
    }

    /**
     * 获取 Header 名称
     *
     * @return Header 名称，如 Content-Type
     */
    public String value() {
        return value;
    }

}
