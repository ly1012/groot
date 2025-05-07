package com.liyunx.groot.protocol.http.config;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.support.Customizer;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Http 配置上下文，包含多个同类型的子配置（多个服务）。
 *
 * <pre>
 *  http:
 *    any:
 *      baseUrl: https://httpbin.org
 *    userservice:
 *      baseUrl: https://xxx.com
 * </pre>
 */
@KeyWord(HttpConfigItem.KEY)
public class HttpConfigItem extends HashMap<String, HttpServiceConfigItem> implements ConfigItem<HttpConfigItem> {

    public static final String KEY = "http";

    public static final String ANY_SERVICE = "any";

    @Override
    public HttpConfigItem merge(HttpConfigItem other) {
        // 以当前对象的值拷贝为基础进行合并
        HttpConfigItem res = this.copy();

        // 参数为 null
        if (other == null) {
            return res;
        }

        // 合并所有存在的 Key
        Set<String> keys = new HashSet<>(this.keySet());
        keys.addAll(other.keySet());

        // 合并值，参数的值覆盖当前对象的值
        for (String key : keys) {
            HttpServiceConfigItem oldItem = this.get(key);
            HttpServiceConfigItem newItem = other.get(key);
            if (oldItem != null && newItem != null) {
                res.put(key, oldItem.merge(newItem));
            } else if (newItem != null) {
                res.put(key, newItem.copy());
            }
        }

        return res;
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();
        this.values().forEach(r::append);
        return r;
    }

    @Override
    public HttpConfigItem copy() {
        //Map 为浅拷贝
        HttpConfigItem httpConfig = new HttpConfigItem();
        //进行深拷贝
        this.forEach((key, item) -> httpConfig.put(key, item.copy()));
        return httpConfig;
    }


    /**
     * {@link HttpConfigItem} Builder，Http 配置上下文构建。
     */
    public static class Builder {

        private final HttpConfigItem httpConfig = new HttpConfigItem();

        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 任意服务（名称为 any）的配置
         *
         * @param any 服务配置
         * @return 本类当前对象
         */
        public Builder anyService(Customizer<HttpServiceConfigItem.Builder> any) {
            HttpServiceConfigItem.Builder builder = HttpServiceConfigItem.Builder.newBuilder();
            any.customize(builder);
            httpConfig.put(HttpConfigItem.ANY_SERVICE, builder.build());
            return this;
        }

        public Builder anyService(@DelegatesTo(strategy= Closure.DELEGATE_ONLY, value = HttpServiceConfigItem.Builder.class) Closure<?> cl) {
            HttpServiceConfigItem.Builder builder = HttpServiceConfigItem.Builder.newBuilder();
            GroovySupport.call(cl, builder);
            httpConfig.put(HttpConfigItem.ANY_SERVICE, builder.build());
            return this;
        }

        /**
         * 特定服务（名称为 serviceName 参数的值）的配置
         *
         * @param serviceName 服务名称
         * @param service     服务配置
         * @return 本类当前对象
         */
        public Builder service(String serviceName, Customizer<HttpServiceConfigItem.Builder> service) {
            HttpServiceConfigItem.Builder builder = HttpServiceConfigItem.Builder.newBuilder();
            service.customize(builder);
            httpConfig.put(serviceName, builder.build());
            return this;
        }

        public HttpConfigItem build() {
            return httpConfig;
        }

    }

}
