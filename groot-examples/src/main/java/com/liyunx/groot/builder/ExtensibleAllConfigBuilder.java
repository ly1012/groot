package com.liyunx.groot.builder;

import com.liyunx.groot.protocol.http.config.HttpConfigItem;
import com.liyunx.groot.support.GroovySupport;
import groovy.lang.Closure;

/**
 * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br>
 * 所有配置构建，适用于 TestCase 或各种 Controller 中间层
 */
public abstract class ExtensibleAllConfigBuilder<T extends ExtensibleAllConfigBuilder<T>>
    extends ExtensibleCommonConfigBuilder<T> {

  // ---------------------------------------------------------------------
  // 新增项目中用到的私有配置项（特定测试元件的配置，公共配置项应在父类中声明）
  // ---------------------------------------------------------------------

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

  private void setHttpConfigItem(
      com.liyunx.groot.protocol.http.config.HttpConfigItem httpConfigItem) {

    config.put(HttpConfigItem.KEY, httpConfigItem);
  }
}
