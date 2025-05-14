package com.liyunx.groot.builder;

import com.liyunx.groot.protocol.http.config.HttpConfigItem;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.testelement.AbstractTestElement.ConfigBuilder;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * <b>本类用于测试，同时也用于资源文件生成，不可随意更改，亦不可删除</b>
 *
 * <p>所有配置构建，适用于 TestCase 或各种 Controller 中间层</p>
 */
public class ExtensibleAllConfigBuilder extends ConfigBuilder<ExtensibleAllConfigBuilder> {

    /**
     * HTTP 配置
     *
     * @param http HTTP 配置 Builder
     * @return 当前配置上下文对象
     */
    public ExtensibleAllConfigBuilder http(Customizer<HttpConfigItem.Builder> http) {
        HttpConfigItem.Builder builder = HttpConfigItem.Builder.newBuilder();
        http.customize(builder);
        setHttpConfigItem(builder.build());
        return this;
    }

    public ExtensibleAllConfigBuilder http(
        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpConfigItem.Builder.class) Closure<?> cl) {

        HttpConfigItem.Builder builder = HttpConfigItem.Builder.newBuilder();
        GroovySupport.call(cl, builder);
        setHttpConfigItem(builder.build());
        return this;
    }

    private void setHttpConfigItem(HttpConfigItem httpConfigItem) {
        config.put(HttpConfigItem.KEY, httpConfigItem);
    }

}
