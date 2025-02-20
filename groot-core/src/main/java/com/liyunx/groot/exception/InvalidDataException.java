package com.liyunx.groot.exception;

/**
 * 非法数据异常，表示数据不合法。
 */
public class InvalidDataException extends GrootException{

  public InvalidDataException() {
  }

  public InvalidDataException(String message) {
    super(message);
  }

  public InvalidDataException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidDataException(String messageTemplate, Object... args) {
    super(messageTemplate, args);
  }

  public InvalidDataException(String messageTemplate, Throwable cause, Object... args) {
    super(messageTemplate, cause, args);
  }

}
