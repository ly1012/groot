package com.liyunx.groot.dataloader;

import com.liyunx.groot.exception.GrootException;

/**
 * 数据加载异常
 */
public class DataLoadException extends GrootException {

  public DataLoadException(){
    super();
  }

  public DataLoadException(String message){
    super(message);
  }

  public DataLoadException(String messageTemplate, Object... args) {
    super(messageTemplate, args);
  }

  public DataLoadException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataLoadException(String messageTemplate, Throwable cause, Object... args) {
    super(messageTemplate, cause, args);
  }
}
