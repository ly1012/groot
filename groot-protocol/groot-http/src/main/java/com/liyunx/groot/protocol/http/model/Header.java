package com.liyunx.groot.protocol.http.model;

import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.liyunx.groot.protocol.http.constants.MediaType;
import com.liyunx.groot.util.FileUtil;

import static com.liyunx.groot.protocol.http.constants.HttpHeader.CONTENT_TYPE;

/**
 * Http Header：单次请求的一行 Header 数据
 */
public class Header extends NameValue<String> implements Copyable<Header>, Computable<Header> {

    public Header() {
    }

    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static Header of(HttpHeader headerName, String headerValue) {
        return new Header(headerName.value(), headerValue);
    }

    /**
     * 根据文件名自动计算 Content-Type Header
     *
     * @param fileName 文件
     * @return Content-Type Header
     */
    public static Header createContentTypeHeader(String fileName) {
        String extName = FileUtil.getExtension(fileName);
        String mediaType = MediaType.getMediaTypeByExtensionName(extName);
        return Header.of(CONTENT_TYPE, mediaType);
    }

    @Override
    public Header copy() {
        Header res = new Header();
        res.name = name;
        res.value = value;
        return res;
    }

    @Override
    public Header eval(ContextWrapper ctx) {
        // name 不支持模板字符串
        if (value == null) {
            value = "";
        } else {
            value = ctx.evalAsString(value);
        }
        return this;
    }

    @Override
    public String toString() {
        return "Header{" +
            "name=" + name +
            ", value=" + value +
            "}";
    }

}
