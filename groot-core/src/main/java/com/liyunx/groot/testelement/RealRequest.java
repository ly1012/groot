package com.liyunx.groot.testelement;

import com.liyunx.groot.exception.GrootException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 测试元件实际发送的请求数据
 */
public class RealRequest {

    /**
     * 请求正文如果不是文件，则值为请求正文内容；如果是文件，则值为 null，除非调用了 {@link #getBody()} 方法
     * <p>
     * 对于特殊的 Body 内容，比如 Multipart 形式或特定的非文本格式，
     * 应当同时使用额外的单独的字段来存储，如 MultiPart multipart，以方便操作。
     * <p>
     * 对于 HTTP 请求，该字段的值为 Request Body 部分。
     */
    protected String body = "";

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
