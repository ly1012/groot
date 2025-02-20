package com.liyunx.groot.protocol.http;


import com.liyunx.groot.protocol.http.model.HttpRequest;
import com.liyunx.groot.protocol.http.okhttp.OkHttpExecutor;

/**
 * Http 请求执行接口
 */
public interface HttpExecutor {

  HttpExecutor defaultExecutor = new OkHttpExecutor();

  /**
   * 执行 Http 请求
   * @param httpRequest 请求数据
   */
  void execute(HttpRequest httpRequest, HttpSampleResult result);

}
