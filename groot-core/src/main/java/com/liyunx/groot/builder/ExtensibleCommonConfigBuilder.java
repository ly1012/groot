package com.liyunx.groot.builder;

import com.liyunx.groot.testelement.AbstractTestElement;

/**
 * 额外的公共配置构建
 *
 * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共配置项（项目中 groot-core 以外的 Jar）。
 */
public abstract class ExtensibleCommonConfigBuilder<T extends ExtensibleCommonConfigBuilder<T>>
    extends AbstractTestElement.ConfigBuilder<T> {

    // ---------------------------------------------------------------------
    // 增加额外的公共配置项
    // ---------------------------------------------------------------------


}
