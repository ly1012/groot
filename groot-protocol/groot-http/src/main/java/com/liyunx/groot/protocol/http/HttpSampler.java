package com.liyunx.groot.protocol.http;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.builder.*;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.dataloader.DataLoader;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.config.HttpConfigItem;
import com.liyunx.groot.protocol.http.config.HttpServiceConfigItem;
import com.liyunx.groot.protocol.http.model.HttpRequest;
import com.liyunx.groot.protocol.http.processor.assertion.matchers.HttpBodyMatcherAssertion;
import com.liyunx.groot.protocol.http.processor.assertion.matchers.HttpHeaderMatcherAssertion;
import com.liyunx.groot.protocol.http.processor.assertion.matchers.HttpStatusCodeMatcherAssertion;
import com.liyunx.groot.protocol.http.processor.extractor.HttpHeaderExtractor;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.support.Ref;
import com.liyunx.groot.testelement.AbstractTestElement;
import com.liyunx.groot.testelement.sampler.AbstractSampler;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Http 协议请求
 * <p>
 * TODO Cookie 保持（用例隔离）：自动保存（Response.Header.Set-Cookie）和发送 Cookie（Request.Header.Cookie）
 * Okhttp 的 CookieJar（基于 OkhttpClient） 好像没法做到用例隔离，考虑做成 Cookie 自动保持插件的形式，需要考虑用例引用的情况，
 * 与用例日志隔离类似，基于线程 ThreadLocal 和 TestCase ID，基于 SessionRunner？
 * <p>
 * TODO Response Data In Memory：设定最大大小限制和保存开关？
 */
@KeyWord(HttpSampler.KEY)
public class HttpSampler extends AbstractSampler<HttpSampler, HttpSampleResult> {

    private static final Logger log = LoggerFactory.getLogger(HttpSampler.class);

    public static final String KEY = "http";

    @JSONField(name = KEY, ordinal = 6)
    protected HttpRequest request;

    public HttpSampler() {
    }

    private HttpSampler(Builder builder) {
        super(builder);
        this.request = builder.request;
    }

    @Override
    protected void init(SessionRunner session) {
        super.init(session);
        // 加载并合并 api 或 template 到当前 sampler
        DataLoader dataLoader = session.getConfiguration().getDataLoader();
        merge(dataLoader);
    }

    @Override
    public HttpSampler copy() {
        HttpSampler self = super.copy();
        self.request = request;
        return self;
    }

    @Override
    public void recover(SessionRunner session) {
        super.recover(session);
        running.request = request.copy();
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();
        if (!disabled && request == null) {
            r.append("\n请求内容不能为空");
        }
        r.append(request);
        return r;
    }

    @Override
    protected HttpSampleResult createTestResult() {
        return new HttpSampleResult();
    }

    @Override
    protected void sample(ContextWrapper contextWrapper, HttpSampleResult result) {
        // 请求数据自动补全
        running.request.autoComplete(contextWrapper);

        // 请求数据合法性检查
        ValidateResult checkResult = running.request.check();
        if (!checkResult.isValid()) {
            throw new InvalidDataException("发送请求前校验失败，最终请求数据不合法，当前测试元件名称：%s%s",
                running.name,
                checkResult.getReason());
        }

        // 发起 HTTP 请求
        HttpExecutor.defaultExecutor.execute(running.request, result);
        logStat(result);
        logRequest(result);
        logResponse(result);
    }

    private void logStat(HttpSampleResult result) {
        if (log.isDebugEnabled()) {
            result.getStat().prettyPrint();
        }
    }

    private void logRequest(HttpSampleResult result) {
        HttpRealRequest realRequest = result.getRequest();
        StringBuilder logBuilder = new StringBuilder();

        // 请求行
        logBuilder
            .append(realRequest.getMethod()).append(" ")
            .append(realRequest.getUrl()).append(" ")
            .append(realRequest.getProtocol().toUpperCase())
            .append("\n");

        // 请求头
        realRequest.getHeaders().forEach(header -> logBuilder
            .append(header.getName()).append(": ")
            .append(header.getValue()).append("\n"));

        logBuilder.append("\n");

        // 请求体
        if (realRequest.isFile()) {
            logBuilder.append("<").append(realRequest.getBodyFile().getAbsolutePath()).append(">");
        } else {
            String body = realRequest.getBody();
            logBuilder.append(Objects.requireNonNullElse(body, "<none>"));
        }

        log.info("实际请求：\n{}", logBuilder);
    }

    private void logResponse(HttpSampleResult result) {
        HttpRealResponse realResponse = result.getResponse();
        StringBuilder logBuilder = new StringBuilder();

        // 响应行
        logBuilder
            .append(realResponse.getProtocol().toUpperCase()).append(" ")
            .append(realResponse.getStatus()).append(" ")
            .append(realResponse.getMessage())
            .append("\n");

        // 响应头
        realResponse.getHeaders().forEach(header -> logBuilder
            .append(header.getName()).append(": ")
            .append(header.getValue()).append("\n"));

        logBuilder.append("\n");

        // 响应体
        if (realResponse.isFile()) {
            logBuilder.append("<").append(realResponse.getBodyFile().getAbsolutePath()).append(">");
        } else {
            String body = realResponse.getBody();
            logBuilder.append(Objects.requireNonNullElse(body, "<none>"));
        }

        log.info("实际响应：\n{}", logBuilder);
    }

    @Override
    protected void handleRequest(ContextWrapper contextWrapper, HttpSampleResult result) {

        // 计算 serviceName
        if (running.request.getServiceName() == null) {
            running.request.setServiceName(HttpConfigItem.ANY_SERVICE);
        }

        // 同类配置合并，并获取合并后的 Http 配置上下文
        HttpConfigItem httpConfig = contextWrapper.getConfigGroup().get(HttpConfigItem.KEY);

        // 获取最终的 HttpConfigItem
        HttpServiceConfigItem service = mergeHttpConfigItem(httpConfig);

        // HttpRequest 合并 service（HttpConfigItem）
        running.request.mergeWith(service);

        // 现在，我们拿到了最终（各种合并后）的 HttpRequest 数据，但 HttpRequest 中仍然存在动态数据。
        // 计算 http request 中的动态数据（如 Url/Header/Body 等位置中的动态数据）
        running.request.eval(contextWrapper);

    }

    private void merge(DataLoader dataLoader) {
        // 被引用 HttpSampler 可能为空
        if (request == null) {
            return;
        }

        // merge api data into the current sampler
        Object apiReference = request.getApi();
        if (apiReference != null) {
            //获取被引用的 HttpAPI
            if (apiReference instanceof String) {
                apiReference = dataLoader.loadByID((String) apiReference, HttpAPI.class);
            }
            if (!(apiReference instanceof HttpAPI)) {
                throw new InvalidDataException("http.api 期望是 String 或 HttpAPI 类型，实际是：%s", apiReference.getClass().getName());
            }
            HttpAPI refApi = ((HttpAPI) apiReference);

            //数据合并：url、method、serviceName
            if (request.getUrl() == null)
                request.setUrl(refApi.getUrl());
            if (request.getMethod() == null)
                request.setMethod(refApi.getMethod());
            if (request.getServiceName() == null)
                request.setServiceName(refApi.getServiceName());
            return;
        }

        // merge template data into the current sampler
        Object templateReference = request.getTemplate();
        if (templateReference != null) {
            //获取被引用的 HttpSampler
            if (templateReference instanceof String) {
                templateReference = dataLoader.loadByID((String) templateReference, HttpSampler.class);
            }
            if (!(templateReference instanceof HttpSampler)) {
                throw new InvalidDataException(
                    "http.template 期望是 String 或 HttpSampler 类型，实际是：%s",
                    templateReference.getClass().getName());
            }
            HttpSampler refTemplate = (HttpSampler) templateReference;
            //被引用的 HttpSampler 合并其引用数据
            refTemplate.merge(dataLoader);
            //数据合并：config、setup、http、teardown
            // config
            if (refTemplate.config != null) {
                config = (TestElementConfig) refTemplate.config.merge(config);
            }
            // setup
            if (refTemplate.setupBefore != null) {
                if (setupBefore == null) setupBefore = new ArrayList<>();
                setupBefore.addAll(0, refTemplate.setupBefore);
            }
            if (refTemplate.setupAfter != null) {
                if (setupAfter == null) setupAfter = new ArrayList<>();
                setupAfter.addAll(0, refTemplate.setupAfter);
            }
            // http
            if (refTemplate.request != null) {
                request = refTemplate.request.merge(request);
            }
            // teardown
            if (refTemplate.teardown != null) {
                if (teardown == null) teardown = new ArrayList<>();
                teardown.addAll(0, refTemplate.teardown);
            }
            if (refTemplate.extract != null) {
                if (extract == null) extract = new ArrayList<>();
                extract.addAll(0, refTemplate.extract);
            }
            if (refTemplate.assert_ != null) {
                if (assert_ == null) assert_ = new ArrayList<>();
                assert_.addAll(0, refTemplate.assert_);
            }
        }
    }

    private HttpServiceConfigItem mergeHttpConfigItem(HttpConfigItem httpConfig) {
        String serviceName = running.request.getServiceName();
        if (httpConfig == null) {
            return null;
        }
        if (serviceName.equals(HttpConfigItem.ANY_SERVICE)) {
            return httpConfig.get(HttpConfigItem.ANY_SERVICE);
        }
        // service 合并 any
        HttpServiceConfigItem service = httpConfig.get(serviceName);
        HttpServiceConfigItem any = httpConfig.get(HttpConfigItem.ANY_SERVICE);
        if (any == null) {
            return service;
        }
        return any.merge(service);
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (AbstractHttpSampler)
    // ---------------------------------------------------------------------

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    // ---------------------------------------------------------------------
    // Builder (HttpSampler)
    // ---------------------------------------------------------------------

    public static class Builder extends AbstractSampler.Builder<
        HttpSampler, Builder,
        ConfigBuilder,
        PreProcessorsBuilder,
        PostProcessorsBuilder, ExtractorsBuilder, AssertionsBuilder> {

        private HttpRequest request;

        @Override
        protected ConfigBuilder getConfigBuilder() {
            return new ConfigBuilder();
        }

        @Override
        protected PreProcessorsBuilder getSetupBuilder(ContextWrapper ctx) {
            return new PreProcessorsBuilder(ctx);
        }

        @Override
        protected PostProcessorsBuilder getTeardownBuilder(ContextWrapper ctx) {
            return new PostProcessorsBuilder(this, ctx);
        }

        @Override
        protected ExtractorsBuilder getExtractBuilder(ContextWrapper ctx) {
            return new ExtractorsBuilder(ctx);
        }

        @Override
        protected AssertionsBuilder getAssertBuilder(ContextWrapper ctx) {
            return new AssertionsBuilder(ctx);
        }

        /**
         * HTTP 请求数据
         *
         * @param request HttpRequest Builder
         * @return 当前对象
         */
        public Builder request(Customizer<HttpRequest.Builder> request) {
            HttpRequest.Builder httpRequestBuilder = HttpRequest.Builder.newBuilder();
            request.customize(httpRequestBuilder);
            this.request = httpRequestBuilder.build();
            return self;
        }

        public Builder request(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpRequest.Builder.class) Closure<?> cl) {
            HttpRequest.Builder httpRequestBuilder = HttpRequest.Builder.newBuilder();
            GroovySupport.call(cl, httpRequestBuilder);
            this.request = httpRequestBuilder.build();
            return self;
        }

        @Override
        public HttpSampler build() {
            return new HttpSampler(this);
        }

    }

    /**
     * HttpSampler 配置构建
     */
    public static class ConfigBuilder extends ExtensibleCommonConfigBuilder<ConfigBuilder> {

        public static ConfigBuilder newBuilder() {
            return new ConfigBuilder();
        }

        /**
         * HTTP 配置
         *
         * @param http HTTP 配置 Builder
         * @return 当前配置上下文对象
         */
        public ConfigBuilder http(Customizer<HttpConfigItem.Builder> http) {
            HttpConfigItem.Builder builder = HttpConfigItem.Builder.newBuilder();
            http.customize(builder);
            setHttpConfigItem(builder.build());
            return this;
        }

        public ConfigBuilder http(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpConfigItem.Builder.class) Closure<?> cl) {
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
        public ConfigBuilder http(HttpConfigItem.Builder http) {
            setHttpConfigItem(http.build());
            return this;
        }

        /**
         * HTTP 配置
         *
         * @param http HTTP 配置
         * @return 当前配置上下文对象
         */
        public ConfigBuilder http(HttpConfigItem http) {
            setHttpConfigItem(http);
            return this;
        }

        private void setHttpConfigItem(HttpConfigItem httpConfigItem) {
            config.put(HttpConfigItem.KEY, httpConfigItem);
        }

    }

    /**
     * 增加 Http 协议特有的前置处理器
     */
    public static class PreProcessorsBuilder extends ExtensibleCommonPreProcessorsBuilder<PreProcessorsBuilder> {

        public PreProcessorsBuilder(ContextWrapper ctx) {
            super(ctx);
        }
    }

    /**
     * 增加 Http 协议特有的后置处理器
     */
    public static class PostProcessorsBuilder extends ExtensibleCommonPostProcessorsBuilder<PostProcessorsBuilder, ExtractorsBuilder, AssertionsBuilder> {

        public PostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder, ContextWrapper ctx) {
            super(elementBuilder, ctx);
        }

        public PostProcessorsBuilder applyR(Consumer<HttpSampleResult> consumer) {
            postProcessors.add(ctx -> consumer.accept((HttpSampleResult) ctx.getTestResult()));
            return self;
        }

    }

    /**
     * 增加 Http 协议特有的提取器
     */
    public static class ExtractorsBuilder extends ExtensibleCommonExtractorsBuilder<ExtractorsBuilder> {

        public ExtractorsBuilder(ContextWrapper ctx) {
            super(ctx);
        }

        public ExtractorsBuilder applyR(Consumer<HttpSampleResult> consumer) {
            extractors.add(ctx -> consumer.accept((HttpSampleResult) ctx.getTestResult()));
            return self;
        }

        /* ------------------------------------------------------------ */
        // HttpHeaderExtractor

        public ExtractorsBuilder header(Ref<String> ref, String headerName) {
            extractors.add(new HttpHeaderExtractor.Builder().ref(ref).headerName(headerName).build());
            return self;
        }

        public ExtractorsBuilder header(Ref<String> ref, String headerName, Customizer<HttpHeaderExtractor.Builder> params) {
            HttpHeaderExtractor.Builder builder = new HttpHeaderExtractor.Builder();
            params.customize(builder);
            extractors.add(builder.ref(ref).headerName(headerName).build());
            return self;
        }

        public ExtractorsBuilder header(Ref<String> ref, String headerName,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpHeaderExtractor.Builder.class) Closure<?> params) {
            HttpHeaderExtractor.Builder builder = new HttpHeaderExtractor.Builder();
            GroovySupport.call(params, builder);
            extractors.add(builder.ref(ref).headerName(headerName).build());
            return self;
        }

        public ExtractorsBuilder header(String refName, String headerName) {
            extractors.add(new HttpHeaderExtractor.Builder().refName(refName).headerName(headerName).build());
            return self;
        }

        public ExtractorsBuilder header(String refName, String headerName, Customizer<HttpHeaderExtractor.Builder> params) {
            HttpHeaderExtractor.Builder builder = new HttpHeaderExtractor.Builder();
            params.customize(builder);
            extractors.add(builder.refName(refName).headerName(headerName).build());
            return self;
        }

        public ExtractorsBuilder header(String refName, String headerName,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HttpHeaderExtractor.Builder.class) Closure<?> params) {
            HttpHeaderExtractor.Builder builder = new HttpHeaderExtractor.Builder();
            GroovySupport.call(params, builder);
            extractors.add(builder.refName(refName).headerName(headerName).build());
            return self;
        }


    }

    /**
     * 增加 Http 协议特有的断言
     */
    public static class AssertionsBuilder extends ExtensibleCommonAssertionsBuilder<AssertionsBuilder> {

        public AssertionsBuilder(ContextWrapper ctx) {
            super(ctx);
        }

        public AssertionsBuilder applyR(Consumer<HttpSampleResult> consumer) {
            assertions.add(ctx -> consumer.accept((HttpSampleResult) ctx.getTestResult()));
            return this;
        }

        /* ------------------------------------------------------------ */
        // 响应状态码断言

        public AssertionsBuilder statusCode(int expected) {
            return statusCode(Matchers.equalTo(expected));
        }

        @SafeVarargs
        public final AssertionsBuilder statusCode(Matcher<? super Integer> matcher, Matcher<? super Integer>... extraMatchers) {
            return statusCode(null, matcher, extraMatchers);
        }

        public AssertionsBuilder statusCode(Function<Integer, ?> mapper,
                                            Matcher<?> matcher, Matcher<?>... extraMatchers) {
            assertions.add(new HttpStatusCodeMatcherAssertion.Builder()
                .mapper(mapper)
                .matchers(matcher, extraMatchers)
                .build());
            return this;
        }

        /* ------------------------------------------------------------ */
        // 响应头断言

        public AssertionsBuilder header(String headerName, String expected) {
            return header(headerName, Matchers.equalTo(expected));
        }

        @SafeVarargs
        public final AssertionsBuilder header(String headerName,
                                              Matcher<? super String> matcher, Matcher<? super String>... extraMatchers) {
            return header(headerName, null, matcher, extraMatchers);
        }

        public AssertionsBuilder header(String headerName,
                                        Function<String, ?> mapper,
                                        Matcher<?> matcher, Matcher<?>... extraMatchers) {
            assertions.add(new HttpHeaderMatcherAssertion.Builder()
                .headerName(headerName)
                .mapper(mapper)
                .matchers(matcher, extraMatchers)
                .build());
            return this;
        }

        /* ------------------------------------------------------------ */
        // 响应体断言

        public AssertionsBuilder body(String expected) {
            return body(Matchers.equalTo(expected));
        }

        @SafeVarargs
        public final AssertionsBuilder body(Matcher<? super String> matcher, Matcher<? super String>... extraMatchers) {
            return body(null, matcher, extraMatchers);
        }

        public AssertionsBuilder body(Function<String, ?> mapper,
                                      Matcher<?> matcher, Matcher<?>... extraMatchers) {
            assertions.add(new HttpBodyMatcherAssertion.Builder()
                .mapper(mapper)
                .matchers(matcher, extraMatchers)
                .build());
            return this;
        }

        //public AssertionsBuilder body(String path, Matcher<?> matcher, Matcher<?>... extraMatchers) {
        //    TODO 通过 threadLocal 获取 ContextWrapper，并根据 content-type 判断，如果缺失，根据 path 判断或 body 内容判断
        //    JsonPathOrXPathContextAwareMapping mapping = new JsonPathOrXPathContextAwareMapping(path);
        //    return body(mapping, matcher, extraMatchers);
        //}


    }

}
