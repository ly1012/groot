package com.liyunx.groot.template.freemarker;

import com.liyunx.groot.exception.GrootException;

/**
 * FreeMarker 解析异常
 */
public class FreeMarkerParseException extends GrootException {

  public FreeMarkerParseException() {
  }

  public FreeMarkerParseException(String message) {
    super(message);
  }

  public FreeMarkerParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public FreeMarkerParseException(Throwable cause) {
    super(cause);
  }
}
