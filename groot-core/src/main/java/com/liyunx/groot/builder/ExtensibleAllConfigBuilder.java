package com.liyunx.groot.builder;

/**
 * 所有配置构建（公共 + 私有），适用于 TestCase 或各种 Controller 中间层
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加所有私有配置项（项目中使用到的 Jar）。
 */
public abstract class ExtensibleAllConfigBuilder<T extends ExtensibleAllConfigBuilder<T>>
    extends ExtensibleCommonConfigBuilder<T> {

    // ---------------------------------------------------------------------
    // 新增项目中用到的私有配置项（父类中仅有公共配置项）
    // ---------------------------------------------------------------------


}
