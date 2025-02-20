package com.liyunx.groot.processor.extractor;

import com.liyunx.groot.processor.ProcessResult;

public class ExtractResult extends ProcessResult {

    /**
     * 提取结果
     */
    private Object value;

    /**
     * 提取失败时的异常对象
     */
    private Exception exception;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
