package com.liyunx.groot.testelement;

import com.liyunx.groot.exception.GrootException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 测试元件实际返回的响应数据
 *
 * <p>
 * 为什么使用嵌套对象的形式存储数据，这里有几点考虑：
 * <ol>
 *   <li>可以方便的通过表达式进行取值，层级明确，如 ${r.time} 或 ${r.response.body}，比 ${r.responseBody} 要清晰</li>
 *   <li>在提取器中，可以通过代码访问必有的字段，如 JSONPath 提取器中获取返回值 r.getResponse().getBody()</li>
 *   <li>允许测试元件扩展与元件有关的字段，如 HttpRealResponse extends RealResponse，使用如 ${r.response.headers}</li>
 * </ol>
 */
public class RealResponse {

    /**
     * 如果没保存到文件，则值为响应体内容；如果保存到了文件，则值为 null，除非调用了 {@link #getBody()} 方法
     * <p>
     * 对于特殊的 Body 内容，比如响应值是 Multipart 形式或特定的非文本格式，
     * 应当同时使用额外的单独的字段来存储，如 MultiPart multipart，以方便提取、断言等操作。
     *
     * <p>对于 HTTP 请求，该字段的值为 Response Body 部分。
     */
    protected String body;

    /**
     * 如果 body 过大（比如超过 1 M），应当使用文件形式存储
     */
    protected File bodyFile;

    /**
     * body 是否为文件，如果是则为 true
     */
    protected boolean file;

    // == Getter/Setter ==

    public String getBody() {
        if (body != null) {
            return body;
        }
        if (bodyFile != null) {
            try {
                body = Files.readString(bodyFile.toPath());
            } catch (IOException e) {
                throw new GrootException(e);
            }
        }
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public File getBodyFile() {
        return bodyFile;
    }

    public void setBodyFile(File bodyFile) {
        this.bodyFile = bodyFile;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(boolean file) {
        this.file = file;
    }

}
