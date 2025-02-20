package com.liyunx.groot.processor.extractor;

import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.model.TestStatus;

/**
 * 提取异常
 *
 * <p>当未显式设置处理器结果时，
 * 抛出该异常表示提取器执行结果为 {@link TestStatus#FAILED} 状态，其他异常将标记为 {@link TestStatus#BROKEN} 状态
 */
public class ExtractException extends GrootException {

    public ExtractException() {
    }

    public ExtractException(String message) {
        super(message);
    }

    public ExtractException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtractException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ExtractException(String messageTemplate, Throwable cause, Object... args) {
        super(messageTemplate, cause, args);
    }

}
