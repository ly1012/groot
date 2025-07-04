package com.liyunx.groot.protocol.http;

import com.liyunx.groot.protocol.http.model.HttpRequest;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.support.Customizer;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.Map;

import static com.liyunx.groot.SessionRunner.getSession;

public class HttpVirtualRunner {

    /* ------------------------------------------------------------ */
    // 直接执行 HTTP 请求，不使用配置和前后置

    public static HttpSampleResult http(String name, Customizer<HttpRequest.Builder> it) {
        HttpSampler.Builder builder = new HttpSampler.Builder();
        builder.name(name);
        builder.request(it);
        return getSession().run(builder.build());
    }

    public static HttpSampleResult http(String name,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpRequest.Builder.class) Closure<?> cl) {
        HttpSampler.Builder builder = new HttpSampler.Builder();
        builder.name(name);
        builder.request(cl);
        return getSession().run(builder.build());
    }

    /* ------------------------------------------------------------ */
    // 执行 HTTP 请求，使用配置和前后置

    public static HttpSampleResult httpWith(String name, HttpSampler sampler) {
        sampler.setName(name);
        return getSession().run(sampler);
    }

    public static HttpSampleResult httpWith(String name, Customizer<HttpSampler.Builder> it) {
        HttpSampler.Builder builder = new HttpSampler.Builder();
        it.customize(builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static HttpSampleResult httpWith(String name,
                                            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpSampler.Builder.class) Closure<?> cl) {
        HttpSampler.Builder builder = new HttpSampler.Builder();
        GroovySupport.call(cl, builder);
        builder.name(name);
        return getSession().run(builder.build());
    }

    public static HttpSampleResult httpWith(String name,
                                            Customizer<HttpSampler.Builder>[] otherCustomizers,
                                            Customizer<HttpRequest.Builder> requestCustomizer) {
        return httpWith(name, builder -> {
            builder.request(requestCustomizer);
            if (otherCustomizers != null) {
                for (Customizer<HttpSampler.Builder> customizer : otherCustomizers) {
                    customizer.customize(builder);
                }
            }
        });
    }

    public static HttpSampleResult httpWith(String name,
                                            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpSampler.Builder.class) Closure<?>[] otherClosures,
                                            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpRequest.Builder.class) Closure<?> requestClosure) {
        return httpWith(name, builder -> {
            builder.request(requestClosure);
            if (otherClosures != null) {
                for (Closure<?> closure : otherClosures) {
                    GroovySupport.call(closure, builder);
                }
            }
        });
    }

    //引用 HttpSampler 模板的快捷写法
    public static HttpSampleResult httpWith(String name, HttpSampler template, Map<String, ?> variables) {
        HttpSampler.Builder builder = new HttpSampler.Builder();
        builder.name(name);
        builder.config(it -> it
            .variables(vars -> {
                    if (variables != null) variables.forEach(vars::var);
                }
            ));
        builder.request(it -> it.withTemplate(template));
        return getSession().run(builder.build());
    }

    public static HttpSampleResult httpWith(String name, HttpSampler template, Customizer<HttpSampler.Builder> it) {
        // 构建 HttpSampler
        HttpSampler.Builder builder = new HttpSampler.Builder();
        it.customize(builder);
        builder.name(name);
        HttpSampler sampler = builder.build();

        // 将 template 加入 HttpSampler
        HttpRequest request = sampler.getRequest();
        if (request == null) {
            request = new HttpRequest();
        }
        request.setTemplate(template);
        sampler.setRequest(request);

        return getSession().run(sampler);
    }

}
