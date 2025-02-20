package com.liyunx.groot.exception;

/**
 * 初始化异常，用例真正执行前出错，比如配置非法或为空。
 */
public class InitializationException extends GrootException {

    public InitializationException() {
    }

    public InitializationException(String message) {
        super(message);
    }

}
