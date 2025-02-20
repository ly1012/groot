package com.liyunx.groot.processor;

import com.liyunx.groot.model.TestStatus;

public class ProcessResult {

    private TestStatus status = TestStatus.PASSED;

    /**
     * Processor 名称
     */
    private String name;

    /**
     * 描述信息，处理器具体做了什么
     *
     * @see Processor#description()
     */
    private String description;

    /**
     * 失败时的异常信息
     */
    private String message;

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
