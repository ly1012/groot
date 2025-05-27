package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.liyunx.groot.protocol.http.support.HttpModelSupport;

import static com.liyunx.groot.protocol.http.constants.HttpHeader.CONTENT_DISPOSITION;
import static com.liyunx.groot.protocol.http.constants.HttpHeader.CONTENT_TYPE;
import static java.util.Objects.isNull;

/**
 * Part，当请求 Body 为 {@link MultiPart} 时，Part 为 MultiPart 的一部分。
 */
public class Part implements Copyable<Part>, Computable<Part> {

    /**
     * Part 的唯一性标记，一般为 {@link HttpHeader#CONTENT_DISPOSITION} 的 name。
     * <br>该字段仅在合并 MultiPart 时使用，会覆盖同名的 Part。
     */
    @JSONField(name = "name")
    private String name;

    /**
     * Part Headers，支持多值 Header
     */
    @JSONField(name = "headers", deserializeUsing = HeaderManager.HeaderManagerObjectReader.class)
    private HeaderManager headers;

    /**
     * any data: byte[]/File/String/Object
     * <p>最终类型是指经过计算后的类型，比如 <code>${toFile('xxx.jpg')}</code> 传入类型是 String，但计算后的类型为 File，即最终类型为 File
     * <pre><code>
     *
     * 最终类型   ->  转换后类型
     * -----------------------
     * byte[]    ->  byte[]
     * File      ->  File
     * String    ->  String
     * Object    ->  JSONString
     * </code></pre>
     */
    @JSONField(name = "body")
    private Object body;

    /**
     * 文件路径，相对路径或绝对路径，支持模板字符串
     * <p>该字段仅为输入数据字段，最终执行前会被合并到 body 字段</p>
     */
    @JSONField(name = "file")
    private String file;

    public Part() {
    }

    /**
     * 创建 Part
     *
     * @param name    Part 名称
     * @param headers Part Headers
     * @param body    Part Body
     * @return Part 对象
     */
    public static Part of(String name, HeaderManager headers, Object body) {
        Part part = new Part();
        part.name = name;
        part.headers = headers;
        part.body = body;
        return part;
    }

    /**
     * 创建 Part
     *
     * @param name    Part 名称
     * @param headers Part Headers
     * @param file    Part Body：文件路径，相对路径或绝对路径，支持模板字符串
     * @return Part 对象
     */
    public static Part ofFile(String name, HeaderManager headers, String file) {
        Part part = new Part();
        part.name = name;
        part.headers = headers;
        part.file = file;
        return part;
    }

    /**
     * 创建通用格式的 Part Headers
     * <pre><code>
     *    Content-Disposition: form-data; name="<name>"[; filename="<filename>"]
     *    Content-Type: <contentType>
     * </code></pre>
     *
     * @param name        Part 名称
     * @param filename    文件名称，可以为 null
     * @param contentType Content-Type 值
     * @return 通用格式的 Part Headers
     */
    public static HeaderManager createPartHeaders(String name, String filename, String contentType) {
        HeaderManager headers = new HeaderManager();
        headers.add(Part.createDispositionHeader(name, filename));
        headers.add(Header.of(CONTENT_TYPE, contentType));
        return headers;
    }

    /**
     * 自动计算 Content-Disposition Header
     * <pre><code>
     *    Content-Disposition: form-data; name="<name>"[; filename="<filename>"]
     * </code></pre>
     *
     * @param name     Part 名称（Control Name）
     * @param filename 文件名称，为 null 时，不追加到值
     * @return Content-Disposition Header
     */
    public static Header createDispositionHeader(String name, String filename) {
        return Header.of(CONTENT_DISPOSITION, dispositionHeaderValue(name, filename));
    }

    /**
     * 自动计算 Content-Disposition Header
     * <pre><code>
     *    Content-Disposition: form-data; name="<name>"[; filename="<filename>"]
     * </code></pre>
     *
     * @param name     Part 名称（Control Name）
     * @param filename 文件名称，为 null 时，不追加到值
     * @return Content-Disposition Header Value
     */
    public static String dispositionHeaderValue(String name, String filename) {
        StringBuilder dispositionBuilder = new StringBuilder();
        dispositionBuilder
            .append("form-data; name=")
            .append(getQuotedString(name));

        if (filename != null) {
            dispositionBuilder
                .append("; filename=")
                .append(getQuotedString(filename));
        }
        return dispositionBuilder.toString();
    }

    // 返回 Key 的双引号包裹后的字符串
    // 换行和双引号将被 URL 编码
    private static String getQuotedString(String key) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"");
        for (char c : key.toCharArray()) {
            switch (c) {
                case '\n':
                    builder.append("%0A");
                    break;
                case '\r':
                    builder.append("%0D");
                    break;
                case '"':
                    builder.append("%22");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        builder.append("\"");
        return builder.toString();
    }

    @Override
    public Part copy() {
        Part res = new Part();
        res.name = name;
        res.headers = isNull(headers) ? null : headers.copy();
        res.body = HttpModelSupport.bodyCopy(body, "http.multipart.[@name='" + name + "'].body");
        res.file = file;
        return res;
    }

    @Override
    public Part eval(ContextWrapper ctx) {
        ctx.eval(headers);
        body = ctx.eval(body);
        if (file != null) {
            file = String.valueOf(ctx.eval(file));
        }
        return this;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HeaderManager getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderManager headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "Part{" +
            "name='" + name + '\'' +
            ", headers=" + headers +
            ", body=" + body +
            ", file='" + file + '\'' +
            '}';
    }
}
