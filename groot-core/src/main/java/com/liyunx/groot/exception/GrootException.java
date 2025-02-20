package com.liyunx.groot.exception;

/**
 * Groot 自定义异常基类
 */
public class GrootException extends RuntimeException {

    public GrootException() {
    }

    public GrootException(String message) {
        super(message);
    }

    public GrootException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrootException(String messageTemplate, Object... args) {
        // TODO 换成 MessageFormat?
        this(String.format(messageTemplate, args));
    }

    public GrootException(String messageTemplate, Throwable cause, Object... args) {
        this(String.format(messageTemplate, args), cause);
    }

    public GrootException(Throwable cause) {
        super(cause);
    }

}
