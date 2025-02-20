package com.liyunx.groot.builder;

import com.liyunx.groot.protocol.http.config.HttpConfigItem;
import com.liyunx.groot.support.GroovySupport;
import groovy.lang.Closure;

/**
 * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br>
 * 所有配置构建，适用于 TestCase 或各种 Controller 中间层
 */
public class ExtensibleAllConfigBuilder
    extends ExtensibleCommonConfigBuilder<ExtensibleAllConfigBuilder> {

  /**
   * HTTP 配置
   *
   * @param http HTTP 配置 Builder
   * @return 当前配置上下文对象
   */
  public com.liyunx.groot.builder.ExtensibleAllConfigBuilder http(
      com.liyunx.groot.support.Customizer<
              com.liyunx.groot.protocol.http.config.HttpConfigItem.Builder>
          http) {

    HttpConfigItem.Builder builder = HttpConfigItem.Builder.newBuilder();
    http.customize(builder);
    setHttpConfigItem(builder.build());
    return this;
  }

  public com.liyunx.groot.builder.ExtensibleAllConfigBuilder http(
      @groovy.lang.DelegatesTo(
              strategy = Closure.DELEGATE_ONLY,
              value = HttpConfigItem.Builder.class)
          groovy.lang.Closure<?> cl) {

    HttpConfigItem.Builder builder = HttpConfigItem.Builder.newBuilder();
    GroovySupport.call(cl, builder);
    setHttpConfigItem(builder.build());
    return this;
  }

  /**
   * HTTP 配置
   *
   * @param http HTTP 配置 Builder
   * @return 当前配置上下文对象
   */
  public com.liyunx.groot.builder.ExtensibleAllConfigBuilder http(
      com.liyunx.groot.protocol.http.config.HttpConfigItem.Builder http) {

    setHttpConfigItem(http.build());
    return this;
  }

  /**
   * HTTP 配置
   *
   * @param http HTTP 配置
   * @return 当前配置上下文对象
   */
  public com.liyunx.groot.builder.ExtensibleAllConfigBuilder http(
      com.liyunx.groot.protocol.http.config.HttpConfigItem http) {

    setHttpConfigItem(http);
    return this;
  }

  private void setHttpConfigItem(
      com.liyunx.groot.protocol.http.config.HttpConfigItem httpConfigItem) {

    config.put(HttpConfigItem.KEY, httpConfigItem);
  }
}
