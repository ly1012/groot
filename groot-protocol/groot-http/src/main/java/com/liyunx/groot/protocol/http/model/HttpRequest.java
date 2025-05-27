package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.common.*;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.HttpAPI;
import com.liyunx.groot.protocol.http.HttpSampler;
import com.liyunx.groot.protocol.http.config.HttpServiceConfigItem;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.liyunx.groot.protocol.http.constants.HttpMethod;
import com.liyunx.groot.protocol.http.constants.MediaType;
import com.liyunx.groot.protocol.http.support.HttpModelSupport;
import com.liyunx.groot.util.KryoUtil;
import com.liyunx.groot.util.StringUtil;
import groovy.lang.Closure;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.liyunx.groot.protocol.http.constants.HttpHeader.CONTENT_DISPOSITION;
import static com.liyunx.groot.protocol.http.constants.HttpHeader.CONTENT_TYPE;
import static com.liyunx.groot.protocol.http.constants.HttpMethod.*;
import static com.liyunx.groot.protocol.http.constants.MediaType.TEXT_PLAIN;
import static com.liyunx.groot.protocol.http.constants.MediaType.getMediaTypeByFileName;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Http 请求数据模型，即 http 关键字属性数据。
 */
public class HttpRequest implements Copyable<HttpRequest>, Mergeable<HttpRequest>, Computable<HttpRequest>, Validatable {

    // == 引用属性（HttpExecutor 用不到） ==

    /**
     * 服务名称，一般情况下，一个 Endpoint 为一个服务
     */
    @JSONField(name = "service")
    private String serviceName;

    /**
     * 引用 {@link HttpAPI} 数据，只引用请求的基本数据，如 method 和 url 等
     * <p>支持两种类型：String（api 标识符） / HttpAPI</p>
     */
    @JSONField(name = "api")
    private Object api;

    /**
     * 引用 {@link HttpSampler} 数据，引用请求模板数据，包含前后置和配置
     * <p>支持两种类型：String（template 标识符） / HttpSampler</p>
     */
    @JSONField(name = "template")
    private Object template;

    // == 请求数据属性 ==

    // >> 请求行 >>
    // baseUrl url vairables 执行前会合并到 url 字段
    /**
     * 基础 URL，即 endpoint，如 <code>https://login.company.com</code>
     */
    @JSONField(name = "baseUrl")
    private String baseUrl;

    /**
     * 请求 URL，可以是相对路径或绝对路径
     */
    @JSONField(name = "url")
    private String url;

    /**
     * 路径变量，使用 <code>:var</code> 表示这是一个路径变量，如：<br>
     * <code>https://httpbin.org/:id</code>
     */
    @JSONField(name = "variables")
    private HashMap<String, String> variables;

    /**
     * 查询参数，<code>https://httpbin.org/get?name=tom&age=20</code> 中的 <code>name=tom&age=20</code> 为查询字符串，
     * <code>name=tom</code> 为一对查询参数。支持多值表示。
     */
    @JSONField(name = "params", deserializeUsing = QueryParamManager.QueryParamManagerObjectReader.class)
    private QueryParamManager params;

    /**
     * 请求方法的字符串表示，如 "GET"
     */
    @JSONField(name = "method")
    private String method;
    // << 请求行 <<

    // >> 请求 Header >>
    /**
     * 请求 Header，支持多值 Header
     */
    @JSONField(name = "headers", deserializeUsing = HeaderManager.HeaderManagerObjectReader.class)
    private HeaderManager headers;

    /**
     * 请求 Cookie（自动转 Header）
     */
    @JSONField(name = "cookies")
    private HashMap<String, String> cookies;
    // << 请求 Header <<

    // auth data
    //@JSONField(name = "basic_auth")   private BasicAuth basicAuth;

    // >> 请求 Body >>
    // body data，提供 5 种类型 Body 字段的理由：
    // 1. 更好的支持配置风格用例（Yaml/Json）
    // 2. 更智能的默认 Content-Type 填充

    /**
     * 任意 Body 类型数据
     * <pre><code>
     * any data: byte[]/File/String/Object
     * 最终类型是指经过计算后的类型，比如 data("${toFile('xxx.jpg')}") 传入类型是 String，但计算后的类型为 File，即最终类型为 File
     *
     * 最终类型   ->  转换后类型   -> 默认 Content-Type
     * ------------------------------------------------------------
     * byte[]    ->  byte[]     -> application/octet-stream
     * File      ->  File       -> 文件后缀名决定，默认 application/octet-stream
     * String    ->  String     -> application/json
     * Object    ->  JSONString -> application/json
     * </code></pre>
     */
    @JSONField(name = "data")
    private Object data;

    /**
     * multipart/form-data 类型数据
     * <p>
     * Content-Type -> default multipart/form-data
     */
    @JSONField(name = "multipart", deserializeUsing = MultiPart.MultiPartObjectReader.class)
    private MultiPart multipart;

    /**
     * application/x-www-form-urlencoded 类型数据
     * <p>
     * Content-Type -> default application/x-www-form-urlencoded
     */
    @JSONField(name = "form", deserializeUsing = FormParamManager.FormParamManagerObjectReader.class)
    private FormParamManager form;

    // json 和 binary 单独拿出来，没有使用 data 来记录，方便特殊处理（配置风格用例）
    // 比如:
    // binary: haveANiceDay.jpg
    // binary: !!binary SmF2YQ==
    // 实际表示： binary: ${toFile('haveANiceDay.jpg')} 和 binary: Java
    // 如果是，
    // data: haveANiceDay.jpg
    // 或
    // data:
    //   file: haveANiceDay.jpg
    // 则无法区分数据为正常的业务数据，还是表示上传文件的意思

    /**
     * JSON 类型数据
     * <p>
     * string/objectToJSONString -> default application/json
     */
    @JSONField(name = "json")
    private Object json;

    /**
     * 二进制数据
     * <p>
     * byte[]/file/stringToFileOrByteArray -> default application/octet-stream
     */
    @JSONField(name = "binary")
    private Object binary;

    @JSONField(name = "bodyType")
    private BodyType bodyType;
    // << 请求 Body <<

    // >> 响应 Body >>
    /**
     * 保存 Response Body 到指定文件
     */
    @JSONField(name = "download")
    private String download;
    // << 响应 Body <<

    // == HTTP 配置属性 ==

    // 其值并非来自于对象的初次构建，而是合并自最终的 HttpConfigItem，即配置属性应该在配置上下文中声明
    @JSONField(deserialize = false)
    private HttpServiceConfigItem httpServiceConfigItem;

    public HttpRequest() {
    }

    private HttpRequest(Builder builder) {
        this.serviceName = builder.serviceName;
        this.api = builder.api;
        this.template = builder.template;

        this.baseUrl = builder.baseUrl;
        this.url = builder.url;
        if (builder.pathVariables.size() > 0)
            this.variables = builder.pathVariables;
        if (builder.queryParams.size() > 0)
            this.params = builder.queryParams;
        this.method = builder.method;

        if (builder.headers.size() > 0)
            this.headers = builder.headers;
        if (builder.cookies.size() > 0)
            this.cookies = builder.cookies;

        this.json = builder.json;
        this.binary = builder.binary;
        this.data = builder.data;
        if (builder.multipart.size() > 0)
            this.multipart = builder.multipart;
        if (builder.form.size() > 0)
            this.form = builder.form;

        this.download = builder.download;
    }


    @Override
    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();
        return r;
    }

    /**
     * 数据合法性检查
     * <p>
     * 该方法和 validate 方法的区别在于此时数据是最终数据（运行时数据检查），
     * 而 validate 检查的是对象构建时的初始数据（用例合法性检查）。
     */
    public ValidateResult check() {
        ValidateResult r = new ValidateResult();

        if (url == null || url.trim().isEmpty()) {
            r.append("\n请求 URL 为空");
        }

        if (method == null || method.trim().isEmpty()) {
            r.append("\nhttp.method 不能为空");
        }

        if (binary != null && !(binary instanceof byte[] || binary instanceof File)) {
            r.append("\nhttp.binary 最终类型必须是 byte[] 或 File\n当前类型：")
                .appendDescription(binary.getClass().getName())
                .appendDescription("\n当前值：")
                .appendDescription(JSON.toJSONString(binary));
        }

        return r;
    }

    @Override
    public HttpRequest copy() {
        HttpRequest res = new HttpRequest();

        // reference
        res.serviceName = serviceName;
        res.api = KryoUtil.copy(api);
        res.template = template instanceof HttpSampler  ? ((HttpSampler) template).copy() : template;

        // url
        res.baseUrl = baseUrl;
        res.url = url;
        res.variables = isNull(variables) ? null : new HashMap<>(variables);
        res.params = isNull(params) ? null : params.copy();

        // method
        res.method = method;

        // header
        res.headers = isNull(headers) ? null : headers.copy();
        res.cookies = isNull(cookies) ? null : new HashMap<>(cookies);

        // body
        res.data = HttpModelSupport.bodyCopy(data, "http.data");
        res.json = HttpModelSupport.bodyCopy(json, "http.json");
        res.binary = HttpModelSupport.bodyCopy(binary, "http.binary");
        res.form = isNull(form) ? null : form.copy();
        res.multipart = isNull(multipart) ? null : multipart.copy();

        res.download = download;

        return res;
    }

    @Override
    public HttpRequest merge(HttpRequest other) {
        if (other == null) {
            return copy();
        }
        HttpRequest res = copy();
        // reference
        if (other.serviceName != null)
            res.serviceName = other.serviceName;
        if (other.api != null)
            res.api = other.api;
        if (other.template != null)
            res.template = other.template;
        // url
        if (other.baseUrl != null)
            res.baseUrl = other.baseUrl;
        if (other.url != null)
            res.url = other.url;
        if (other.variables != null) {
            if (res.variables == null)
                res.variables = new HashMap<>();
            res.variables.putAll(other.variables);
        }
        if (other.params != null) {
            res.params = res.params == null ? other.params.copy() : res.params.merge(other.params);
        }
        // method
        if (other.method != null)
            res.method = other.method;
        // header
        if (other.headers != null) {
            res.headers = res.headers == null ? other.headers.copy() : res.headers.merge(other.headers);
        }
        if (other.cookies != null) {
            if (res.cookies == null)
                res.cookies = new HashMap<>();
            res.cookies.putAll(other.cookies);
        }
        // body
        if (other.data != null) {
            res.data = HttpModelSupport.bodyCopy(other.data, "http.data");
        }
        if (other.multipart != null) {
            res.multipart = res.multipart == null ? other.multipart.copy() : res.multipart.merge(other.multipart);
        }
        if (other.form != null) {
            res.form = res.form == null ? other.form.copy() : res.form.merge(other.form);
        }
        if (other.json != null) {
            if (other.json instanceof String) {
                res.json = other.json;
            } else {
                res.json = KryoUtil.copy(other.json);
            }
        }
        if (other.binary != null) {
            res.binary = HttpModelSupport.bodyCopy(other.binary, "http.binary");
        }

        if (other.download != null) {
            res.download = other.download;
        }

        return res;
    }

    /**
     * 将 Http 配置合并进当前请求（当前请求覆盖配置）
     *
     * @param item Http 请求配置项
     * @return 合并后的请求，原地修改
     */
    public HttpRequest mergeWith(HttpServiceConfigItem item) {
        httpServiceConfigItem = new HttpServiceConfigItem();
        if (item == null) {
            setDefaultValue();
            return this;
        }

        if (baseUrl == null) {
            baseUrl = item.getBaseUrl();
        }

        if (item.getHeaders() != null) {
            headers = (headers == null)
                ? item.getHeaders().copy()
                : item.getHeaders().merge(headers);
        }

        httpServiceConfigItem.setProxy(item.getProxy());
        httpServiceConfigItem.setVerify(item.getVerify());
        httpServiceConfigItem.setRaw(item.getRaw());

        setDefaultValue();

        return this;
    }

    @Override
    public HttpRequest eval(ContextWrapper ctx) {
        // 为什么这里不直接 ctx.eval(this) 进行替换，而是对需要的字段主动 eval？
        // 因为 ctx.eval 方法只能对集合、字符串等简单的对象进行自动计算，而复杂的自定义对象是无法自动计算的，所以这里手动计算。

        // url
        baseUrl = ctx.evalAsString(baseUrl);
        url = ctx.evalAsString(url);
        ctx.eval(variables);            // 路径变量
        ctx.eval(params);               // 路径查询参数

        // headers
        ctx.eval(headers);
        ctx.eval(cookies);              // 特殊 Header: cookies

        // body
        if (!httpServiceConfigItem.getRaw()) {
            data = ctx.eval(data);
            ctx.eval(multipart);
            ctx.eval(form);
            json = ctx.eval(json, true);
            binary = ctx.eval(binary);
        }

        if (download != null) {
            download = ctx.evalAsString(download);
        }

        // http config
        if (httpServiceConfigItem != null) {
            HttpProxy httpProxy = httpServiceConfigItem.getProxy();
            if (httpProxy != null) {
                ctx.eval(httpProxy);
            }
        }

        return this;
    }

    /**
     * 请求数据自动补全与自动转换
     */
    public void autoComplete(ContextWrapper ctx) {
        // 路径变量，url = url + variables
        fillUrlWithPathVariables();

        // 完整 Url（不含 Query Params）
        fillUrlWithBaseUrl();

        // method 转大写
        if (nonNull(method)) {
            method = method.toUpperCase();
        }

        // Cookies 转标准 Header
        fillHeaderWithCookies();

        BodyType bodyType = parseBodyType();

        // 将 Body 数据项转为标准格式：byte[]/File/String
        bodyAutoComplete(ctx, bodyType);

        // 如果 Content-Type 缺失，添加默认类型
        fillHeaderWithContentType(bodyType);

        // download 位置补全
        if (download != null) {
            Path path = Paths.get(download);
            // 路径检查：如果是相对路径，自动拼接得到可访问路径
            if (!path.isAbsolute()) {
                download = Paths.get(ApplicationConfig.getWorkDirectory()).resolve(path).toAbsolutePath().toString();
            }
        }
    }

    /**
     * 根据属性是否为 null 判断 Request Body 类型：json > form > data > multipart > binary > nobody
     *
     * @return Body 类型枚举值
     */
    public BodyType parseBodyType() {
        if (nonNull(bodyType)) {
            return bodyType;
        }

        List<BodyType> presentTypes = new ArrayList<>();
        if (json != null) presentTypes.add(BodyType.JSON);
        if (form != null) presentTypes.add(BodyType.FORM);
        if (data != null) presentTypes.add(BodyType.DATA);
        if (multipart != null) presentTypes.add(BodyType.MULTIPART);
        if (binary != null) presentTypes.add(BodyType.BINARY);
        if (presentTypes.size() > 1) {
            String typeNames = presentTypes.stream()
                .map(BodyType::name)
                .collect(Collectors.joining(", "));
            throw new IllegalStateException("同时存在多个 Body 类型（最多只能存在一个）: " + typeNames);
        }
        bodyType = presentTypes.isEmpty() ? BodyType.NOBODY : presentTypes.get(0);
        return bodyType;
    }

    private void setDefaultValue() {
        // 默认值填充
        if (httpServiceConfigItem.getVerify() == null)
            httpServiceConfigItem.setVerify(true);

        if (httpServiceConfigItem.getRaw() == null)
            httpServiceConfigItem.setRaw(false);
    }

    private void fillUrlWithPathVariables() {
        if (null == url || null == variables) {
            return;
        }

        String name;
        String value;
        String[] splits = url.split("/");
        int len = splits.length;
        for (int i = 0; i < len; i++) {
            name = splits[i];
            if (name.length() > 1 && name.startsWith(":")) {
                name = name.substring(1);
                value = variables.get(name);
                if (value != null) {
                    splits[i] = value;
                }
            }
        }

        url = String.join("/", splits);
    }

    private void fillUrlWithBaseUrl() {
        // baseUrl 去除首尾空格
        if (null != baseUrl) {
            baseUrl = baseUrl.trim();
        }

        // url 去除首尾空格
        if (null != url) {
            url = url.trim();
        }

        // baseUrl 和 url 拼接
        if (null != baseUrl) {
            if (null == url) {
                url = baseUrl;
            } else if (!StringUtil.isHttpOrHttps(url)) {
                url = baseUrl + url;
            }
        }
    }

    private void fillHeaderWithCookies() {
        // cookies 为空则返回，无需填充
        if (null == cookies || cookies.isEmpty()) {
            return;
        }

        // headers 不存在，则实例化一个
        if (null == headers) {
            headers = new HeaderManager();
        }

        // 添加 Cookie 请求头
        headers.setCookies(cookies);
    }

    private void fillHeaderWithContentType(BodyType bodyType) {
        final String CONTENT_TYPE_HEADER = CONTENT_TYPE.value();

        Header contentTypeHeader = null;
        if (headers == null) {
            headers = new HeaderManager();
        } else {
            contentTypeHeader = headers.getHeader(CONTENT_TYPE_HEADER);
        }

        if (contentTypeHeader != null) {
            return;
        }

        switch (bodyType) {
            case JSON:
                headers.add(new Header(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON.value()));
                break;
            case FORM:
                headers.add(new Header(CONTENT_TYPE_HEADER, MediaType.APPLICATION_X_WWW_FORM_URLENCODED.value()));
                break;
            case MULTIPART:
                headers.add(new Header(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA.value()));
                break;
            case BINARY:
                if (binary instanceof File file) {
                    headers.add(Header.createContentTypeHeader(file.getName()));
                } else {
                    headers.add(new Header(CONTENT_TYPE_HEADER, MediaType.APPLICATION_OCTET_STREAM.value()));
                }
                break;
            case DATA:
                if (data instanceof byte[]) {
                    headers.add(new Header(CONTENT_TYPE_HEADER, MediaType.APPLICATION_OCTET_STREAM.value()));
                } else if (data instanceof File file) {
                    headers.add(Header.createContentTypeHeader(file.getName()));
                } else {
                    headers.add(new Header(CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON.value()));
                }
                break;
        }

    }

    /**
     * 自动解析 Body 类型，功能同 {@link #bodyAutoComplete(ContextWrapper, BodyType)}
     */
    public void bodyAutoComplete(ContextWrapper ctx) {
        bodyAutoComplete(ctx, parseBodyType());
    }

    /**
     * <pre><code>
     * 主要功能：将 Body 数据类型转为标准格式 byte[]/File/String
     *
     * 【对于 binary 类型】
     *
     * 1. 如果值为字符串，则当做文件 ID 转为 File
     * 2. 如果值为 Map，且下面 Key 的值为 String 类型，则：
     *    1. file：当做文件 ID 转为 File
     *    2. base64：将 base64 字符串转为 byte[]（为什么不直接传 byte[]？因为 Yaml/Json 中字段值类型不支持 byte[]）
     *
     * 【对于 data 类型】如果值不是 byte[]/File/String 类型，则调用 JSON.toJSONString 方法转为 String。
     *
     * 【对于 json 类型】如果值不是 String 类型，则调用 JSON.toJSONString 方法转为 String。
     *
     * 【对于 multipart 类型】每个 Part 部分：
     *
     * 1. Headers 部分会自动补全 Content-Type 和 Content-Disposition
     * 2. Body 部分会自动转为 byte[]/File/String 类型
     * </code></pre>
     */
    public void bodyAutoComplete(ContextWrapper ctx, BodyType bodyType) {
        switch (bodyType) {
            case DATA:
                if (!isByteArrayOrFileOrString(data)) {
                    data = JSON.toJSONString(data);
                }
                break;

            case MULTIPART:
                for (Part part : multipart) {
                    partAutoComplete(part, ctx);
                }
                break;

            case JSON:
                if (!(json instanceof String)) {
                    json = JSON.toJSONString(json);
                }
                break;

            case BINARY:
                binaryAutoComplete(ctx);
                break;
        }
    }

    private void binaryAutoComplete(ContextWrapper ctx) {
        if (binary instanceof String binaryString) {
            // 此时已调用过 this.eval 方法，如果还是 String 类型，则当做文件 ID 处理
            binary = loadAsFile(ctx, binaryString);
        } else if (binary instanceof Map binaryMap) {
            if (binaryMap.get("file") instanceof String fileId) {
                binary = loadAsFile(ctx, fileId);
            } else if (binaryMap.get("base64") instanceof String base64) {
                binary = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private File loadAsFile(ContextWrapper ctx, String fileId) {
        return ctx.getSessionRunner().getConfiguration()
            .getDataLoader()
            .loadByID(fileId, File.class);
    }

    private void partAutoComplete(Part part, ContextWrapper ctx) {
        String fileId = part.getFile();
        boolean isFile = fileId != null;

        // Part.Name
        String partName = part.getName();
        if (isFile && (partName == null || partName.isEmpty())) {
            partName = "file";
            part.setName(partName);
        }

        // Part.Body 如果存在 file，则优先使用 file
        String filename = null;
        if (isFile) {
            File file = ctx.getSessionRunner().getConfiguration().getDataLoader().loadByID(fileId, File.class);
            if (!file.exists()) {
                throw new InvalidDataException("http.multipart[*].file：文件 %s 不存在", file.getAbsolutePath());
            }
            part.setBody(file);
            filename = file.getName();
        } else {
            if (!isByteArrayOrFileOrString(part.getBody())) {
                part.setBody(JSON.toJSONString(part.getBody()));
            }
        }

        // Part.Headers
        HeaderManager headers = part.getHeaders();
        // 分析指定 Header 是否需要补全
        boolean noDisposition = false;
        boolean noType = false;
        if (headers == null) {
            headers = new HeaderManager();
            noDisposition = true;
            noType = true;
        } else {
            if (headers.getHeader(CONTENT_DISPOSITION) == null) {
                noDisposition = true;
            }
            if (headers.getHeader(CONTENT_TYPE) == null) {
                noType = true;
            }
        }
        // 补全缺失 Header
        if (noDisposition) {
            headers.add(Part.createDispositionHeader(partName, filename));
        }
        if (noType) {
            headers.add(Header.of(CONTENT_TYPE, isFile ? getMediaTypeByFileName(filename) : TEXT_PLAIN.value()));
        }
        part.setHeaders(headers);
    }

    private boolean isByteArrayOrFileOrString(Object object) {
        return object instanceof byte[] || object instanceof File || object instanceof String;
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Object getApi() {
        return api;
    }

    public void setApi(Object api) {
        this.api = api;
    }

    public Object getTemplate() {
        return template;
    }

    public void setTemplate(Object template) {
        this.template = template;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<String, String> getVariables() {
        return variables;
    }

    public void setVariables(HashMap<String, String> variables) {
        this.variables = variables;
    }

    public QueryParamManager getParams() {
        return params;
    }

    public void setParams(QueryParamManager params) {
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public HeaderManager getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderManager headers) {
        this.headers = headers;
    }

    public HashMap<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(HashMap<String, String> cookies) {
        this.cookies = cookies;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public MultiPart getMultipart() {
        return multipart;
    }

    public void setMultipart(MultiPart multipart) {
        this.multipart = multipart;
    }

    public FormParamManager getForm() {
        return form;
    }

    public void setForm(FormParamManager form) {
        this.form = form;
    }

    public Object getJson() {
        return json;
    }

    public void setJson(Object json) {
        this.json = json;
    }

    public Object getBinary() {
        return binary;
    }

    public void setBinary(Object binary) {
        this.binary = binary;
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public String getDownload() {
        return download;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public HttpServiceConfigItem getHttpServiceConfigItem() {
        return httpServiceConfigItem;
    }

    /**
     * Request Body 类型枚举
     */
    public enum BodyType {

        DATA,
        MULTIPART,
        FORM,
        JSON,
        BINARY,
        NOBODY

    }

    /**
     * HttpRequest Builder
     */
    public static class Builder {

        // == 引用属性 ==

        private String serviceName;
        private Object api;             // String/HttpAPI
        private Object template;        // String/HttpSampler

        // == Request Start Line ==

        private String method;

        private String baseUrl;
        private String url;
        private HashMap<String, String> pathVariables = new HashMap<>();     //路径变量
        private QueryParamManager queryParams = new QueryParamManager();     //查询参数

        // == Request Headers ==

        private HeaderManager headers = new HeaderManager();                 //请求 Header
        private HashMap<String, String> cookies = new HashMap<>();           //请求 Cookie（自动转 Header）

        // == Request Body ==

        private Object data;
        private MultiPart multipart = new MultiPart();
        private FormParamManager form = new FormParamManager();
        private Object json;
        private Object binary;

        // == Response Body ==

        private String download;

        public static Builder newBuilder() {
            return new Builder();
        }

        // ---------------------------------------------------------------------
        // 引用属性
        // ---------------------------------------------------------------------

        /**
         * 使用指定服务的配置
         *
         * @param serviceName 服务名称
         * @return 当前对象
         */
        public Builder withService(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * 引用 HTTP API
         *
         * @param identifier HTTP API 定位符
         * @return 当前对象
         */
        public Builder withApi(String identifier) {
            this.api = identifier;
            return this;
        }

        /**
         * 引用 HTTP API
         *
         * @param api HttpAPI 对象
         * @return 当前对象
         */
        public Builder withApi(HttpAPI api) {
            this.api = api;
            return this;
        }

        /**
         * 引用 HttpSampler 模板
         *
         * @param identifier HttpSampler 模板定位符
         * @return 当前对象
         */
        public Builder withTemplate(String identifier) {
            this.template = identifier;
            return this;
        }

        /**
         * 引用 HttpSampler 模板
         *
         * @param template HttpSampler 对象
         * @return 当前对象
         */
        public Builder withTemplate(HttpSampler template) {
            this.template = template;
            return this;
        }


        // ---------------------------------------------------------------------
        // HTTP Method
        // ---------------------------------------------------------------------

        /**
         * HTTP 请求方式
         *
         * @param method 请求方法的 {@link HttpMethod} 枚举表示，如 GET/PUT/DELETE 等
         * @return 当前对象
         */
        public Builder method(HttpMethod method) {
            this.method = method.name();
            return this;
        }

        /**
         * HTTP 请求方式
         *
         * @param method 请求方法的字符串表示，如 GET/PUT/DELETE 等
         * @return 当前对象
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        // ---------------------------------------------------------------------
        // HTTP URL / METHOD + URL
        // ---------------------------------------------------------------------

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * HTTP 请求 URL
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * HTTP 请求方法和 URL
         *
         * @param method 请求方法的字符串表示，如 GET/PUT/DELETE 等
         * @param url    请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder method(String method, String url) {
            this.method = method;
            this.url = url;
            return this;
        }

        /**
         * HTTP 请求方法和 URL
         *
         * @param method 请求方法的枚举表示，如 GET/PUT/DELETE 等
         * @param url    请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder method(HttpMethod method, String url) {
            this.method = method.name();
            this.url = url;
            return this;
        }

        /**
         * GET 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder get(String url) {
            return method(GET, url);
        }

        /**
         * POST 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder post(String url) {
            return method(POST, url);
        }

        /**
         * PUT 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder put(String url) {
            return method(PUT, url);
        }

        /**
         * DELETE 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder delete(String url) {
            return method(DELETE, url);
        }

        /**
         * PATCH 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder patch(String url) {
            return method(PATCH, url);
        }

        /**
         * HEAD 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder head(String url) {
            return method(HEAD, url);
        }

        /**
         * OPTIONS 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder options(String url) {
            return method(OPTIONS, url);
        }

        /**
         * TRACE 请求
         *
         * @param url 请求 URL，可以是绝对 URL 或相对 URL
         * @return 当前对象
         */
        public Builder trace(String url) {
            return method(TRACE, url);
        }

        public Builder connect(String url) {
            return method(CONNECT, url);
        }


        // ---------------------------------------------------------------------
        // 路径变量（或称为 REST 参数，或称为路径参数）
        // ---------------------------------------------------------------------

        /**
         * 路径变量，又称为 REST 参数。
         * <p>
         * <code>https://httpbin.org/get/:version</code> 中的 version 即为路径变量，有些工具中使用 {version} 表示路径变量。
         * </p>
         *
         * @param name  路径变量的名称
         * @param value 路径变量的值
         * @return 当前对象
         */
        public Builder pathVariable(String name, String value) {
            pathVariables.put(name, value);
            return this;
        }

        /**
         * 路径变量
         *
         * @param pathVariables 路径变量键值对，即多个路径变量的 Map 表示
         * @return 当前对象
         * @see #pathVariable(String, String)
         */
        public Builder pathVariables(Map<String, String> pathVariables) {
            if (pathVariables != null) {
                this.pathVariables = new HashMap<>(pathVariables);
            }
            return this;
        }

        // ---------------------------------------------------------------------
        // 查询参数
        // ---------------------------------------------------------------------

        /**
         * 查询参数
         * <p>
         * <code>https://httpbin.org/get?name=tom&age=20</code> 中的 <code>name=tom&age=20</code> 为查询字符串，
         * <code>name=tom</code> 为一对查询参数。
         * </p>
         *
         * @param name  查询参数的名称
         * @param value 查询参数的值
         * @return 当前对象
         */
        public Builder queryParam(String name, String value) {
            this.queryParams.add(new QueryParam(name, value));
            return this;
        }

        /**
         * 查询参数
         *
         * @param name   查询参数的名称
         * @param values 查询参数的值，可以是多个值
         * @return 当前对象
         * @see #queryParam(String, String)
         */
        public Builder queryParam(String name, String... values) {
            if (values == null) {
                this.queryParams.add(new QueryParam(name, null));
            } else {
                for (String value : values) {
                    this.queryParams.add(new QueryParam(name, value));
                }
            }
            return this;
        }

        /**
         * 查询参数
         *
         * @param queryParams 查询参数键值对
         * @return 当前对象
         * @see #queryParam(String, String)
         */
        public Builder queryParams(Map<String, String> queryParams) {
            if (queryParams == null) {
                return this;
            }

            this.queryParams.clear();
            queryParams.forEach((k, v) -> {
                this.queryParams.add(new QueryParam(k, v));
            });
            return this;
        }

        /**
         * 查询参数
         *
         * @param queryParams 查询参数键值对（同名参数支持多个值）
         * @return 当前对象
         * @see #queryParam(String, String)
         */
        public Builder queryParams(QueryParamManager queryParams) {
            if (queryParams == null) {
                return this;
            }
            this.queryParams = queryParams;
            return this;
        }

        // ---------------------------------------------------------------------
        // Http Request Header
        // ---------------------------------------------------------------------

        /**
         * 请求 Header，常见 Header 参考 {@link HttpHeader}，常见媒体类型参考 {@link MediaType}。
         *
         * @param name  Header 名称
         * @param value Header 值
         * @return 当前对象
         */
        public Builder header(String name, String value) {
            this.headers.add(new Header(name, value));
            return this;
        }

        /**
         * 请求 Header
         *
         * @param name   Header 名称
         * @param values Header 值，可以是多个值
         * @return 当前对象
         * @see #header(String, String)
         */
        public Builder header(String name, String... values) {
            if (values == null) {
                this.headers.add(new Header(name, null));
            } else {
                for (String value : values) {
                    this.headers.add(new Header(name, value));
                }
            }
            return this;
        }

        /**
         * 请求 Header
         *
         * @param headers Header 键值对
         * @return 当前对象
         * @see #header(String, String)
         */
        public Builder headers(Map<String, String> headers) {
            if (headers == null) {
                return this;
            }

            this.headers.clear();
            headers.forEach((k, v) -> {
                this.headers.add(new Header(k, v));
            });
            return this;
        }

        public Builder headers(String... nameAndValues) {
            if (nameAndValues == null) {
                return this;
            }
            if (nameAndValues.length % 2 != 0) {
                throw new IllegalArgumentException("键值对必须成对出现");
            }
            this.headers.clear();
            for (int i = 0; i < nameAndValues.length - 1; i += 2) {
                this.headers.add(new Header(nameAndValues[i], nameAndValues[i + 1]));
            }
            return this;
        }

        /**
         * 请求 Header
         *
         * @param headers Header 键值对（同名参数支持多个值）
         * @return 当前对象
         * @see #header(String, String)
         */
        public Builder headers(HeaderManager headers) {
            if (headers == null) {
                return this;
            }
            this.headers = headers;
            return this;
        }

        /**
         * 请求 Cookie
         *
         * @param name  Cookie 名称
         * @param value Cookie 值
         * @return 当前对象
         */
        public Builder cookie(String name, String value) {
            this.cookies.put(name, value);
            return this;
        }

        /**
         * 请求 Cookie
         *
         * @param cookies Cookie 键值对
         * @return 当前对象
         */
        public Builder cookies(Map<String, String> cookies) {
            if (cookies != null) {
                this.cookies = new HashMap<>(cookies);
            }
            return this;
        }

        public Builder cookies(String... nameAndValues) {
            if (nameAndValues == null) {
                return this;
            }
            if (nameAndValues.length % 2 != 0) {
                throw new IllegalArgumentException("键值对必须成对出现");
            }
            this.cookies.clear();
            for (int i = 0; i < nameAndValues.length - 1; i += 2) {
                this.cookies.put(nameAndValues[i], nameAndValues[i + 1]);
            }
            return this;
        }

        // ---------------------------------------------------------------------
        // Http Request Body
        // ---------------------------------------------------------------------

        /**
         * 请求 Body：JSON 字符串
         * <p>
         * 默认 Content-Type: application/json
         *
         * @param body JSON 字符串或模板字符串，如果模板计算结果非 String 类型，序列化为 JSON 字符串
         * @return 当前对象
         */
        public Builder json(String body) {
            this.json = body;
            return this;
        }

        /**
         * 请求 Body：JSON 字符串
         * <p>
         * 默认 Content-Type: application/json
         *
         * @param body 任意对象，计算后序列化为 JSON 字符串
         * @return 当前对象
         */
        public Builder json(Object body) {
            this.json = body;
            return this;
        }

        /**
         * 请求 Body：二进制数据，byte[] 或 File 类型数据
         * <p>
         * 默认 Content-Type: application/octet-stream
         *
         * @param body 模板字符串，计算结果必须是 byte[] 或 File 类型
         * @return 当前对象
         */
        public Builder binary(String body) {
            this.binary = body;
            return this;
        }

        /**
         * 请求 Body：二进制数据
         * <p>
         * 默认 Content-Type: application/octet-stream
         *
         * @param body 二进制数据
         * @return 当前对象
         */
        public Builder binary(byte[] body) {
            this.binary = body;
            return this;
        }

        /**
         * 请求 Body：二进制数据
         * <p>
         * 默认 Content-Type: application/octet-stream
         *
         * @param body 文件
         * @return 当前对象
         */
        public Builder binary(File body) {
            if (body == null || !body.exists()) {
                throw new InvalidDataException("文件缺失或不存在");
            }
            this.binary = body;
            return this;
        }

        /**
         * 请求 Body：application/x-www-form-urlencoded 格式数据
         *
         * @param name  名称
         * @param value 值
         * @return 当前对象
         */
        public Builder formParam(String name, String value) {
            this.form.add(new FormParam(name, value));
            return this;
        }

        /**
         * 请求 Body：application/x-www-form-urlencoded 格式数据
         *
         * @param name   名称
         * @param values 值，支持多个值
         * @return 当前对象
         */
        public Builder formParam(String name, String... values) {
            if (values == null) {
                this.form.add(new FormParam(name, null));
            } else {
                for (String value : values) {
                    this.form.add(new FormParam(name, value));
                }
            }
            return this;
        }

        /**
         * 请求 Body：application/x-www-form-urlencoded 格式数据
         *
         * @param formParams Form Params 数据
         * @return 当前对象
         */
        public Builder formParams(Map<String, String> formParams) {
            if (formParams != null) {
                formParams.forEach((k, v) -> this.form.add(new FormParam(k, v)));
            }
            return this;
        }

        public Builder formParams(String... nameAndValues) {
            if (nameAndValues == null) {
                return this;
            }
            if (nameAndValues.length % 2 != 0) {
                throw new IllegalArgumentException("键值对必须成对出现");
            }
            this.form.clear();
            for (int i = 0; i < nameAndValues.length - 1; i += 2) {
                this.form.add(new FormParam(nameAndValues[i], nameAndValues[i + 1]));
            }
            return this;
        }

        /**
         * 请求 Body：application/x-www-form-urlencoded 格式数据
         *
         * @param formParams Form Params 数据
         * @return 当前对象
         */
        public Builder formParams(FormParamManager formParams) {
            if (formParams == null) {
                return this;
            }
            this.form = formParams;
            return this;
        }

        /**
         * 请求 Body：任意类型数据，优先使用 json/binary/form/multiPart
         * <pre><code>
         * any data: byte[]/File/String/Object
         * 最终类型是指经过计算后的类型，比如 data("${toFile('xxx.jpg')}") 传入类型是 String，但计算后的类型为 File，即最终类型为 File
         *
         * 最终类型   ->  转换后类型   -> 默认 Content-Type
         * ------------------------------------------------------------
         * byte[]    ->  byte[]     -> application/octet-stream
         * File      ->  File       -> 文件后缀名决定，默认 application/octet-stream
         * String    ->  String     -> application/json
         * Object    ->  JSONString -> application/json
         * </code></pre>
         *
         * @param data 任意类型数据
         * @return 当前对象
         */
        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        /**
         * 别名方法
         *
         * @see #data(Object)
         */
        public Builder body(Object data) {
            return data(data);
        }

        /**
         * 使用闭包返回值作为 Body
         *
         * @param cl 返回 Body 数据的闭包
         * @return 当前对象
         */
        public Builder body(Closure<?> cl) {
            return data(cl.call());
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <p>name="file"</p>
         *
         * @see #multiPartFile(String, File)
         */
        public Builder multiPartFile(File file) {
            return multiPartFile("file", file);
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <p>Content-Type 的值根据文件名后缀自动计算，如果无法计算则默认为 application/octet-stream</p>
         *
         * @see #multiPartFile(String, File, String)
         */
        public Builder multiPartFile(String name, File file) {
            return multiPartFile(name, file, getMediaTypeByFileName(file.getName()));
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <p>使用 file.getName() 作为文件名</p>
         *
         * @param name        Part 名称
         * @param file        要上传的文件
         * @param contentType Content-Type 值
         * @return 当前对象
         * @see #multiPartFile(String, String, File, String)
         */
        public Builder multiPartFile(String name, File file, String contentType) {
            return multiPartFile(name, file.getName(), file, contentType);
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <pre><code>
         * // Part Headers
         * Content-Disposition: form-data; name="<name>"; filename="<filename>"
         * Content-Type: <contentType>
         *
         * // Example:
         * Content-Disposition: form-data; name="file"; filename="武功秘籍.pdf"
         * Content-Type: application/pdf
         * </code></pre>
         *
         * @param name        Part 名称
         * @param filename    文件名称
         * @param file        要上传的文件
         * @param contentType Content-Type 值
         * @return 当前对象
         */
        public Builder multiPartFile(String name, String filename, File file, String contentType) {
            if (file == null || !file.exists()) {
                throw new InvalidDataException("文件缺失或不存在");
            }

            HeaderManager headers = Part.createPartHeaders(name, filename, contentType);
            return multiPart(name, headers, file);
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <p>name="file"<br>
         * filename="file.getName()"<br>
         * Content-Type: 根据文件后缀名自动计算，无法计算则为 application/octet-stream</p>
         *
         * @param path 要上传的文件路径，相对路径或绝对路径，支持模板字符串
         * @return 当前对象
         * @see #multiPartFile(String, String)
         */
        public Builder multiPartFile(String path) {
            return multiPartFile("file", path);
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <p>filename="file.getName()"<br>
         * Content-Type: 根据文件后缀名自动计算，无法计算则为 application/octet-stream</p>
         *
         * @param path 要上传的文件路径，相对路径或绝对路径，支持模板字符串
         * @see #multiPartFile(String, String, String)
         */
        public Builder multiPartFile(String name, String path) {
            // path 可能是模板字符串，比如 ${myFilePath}，所以这里无法自动推测 Header 内容，需要执行时再自动补全
            // 而其他方法可以在声明时就确定 Header 内容，所以应提前完成 Header 计算，不应到运行时再自动补全，
            // 即能提前完成的计算，应当提前完成
            multipart.add(Part.ofFile(name, null, path));
            return this;
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <p>filename="file.getName()"</p>
         *
         * @param path 要上传的文件路径，相对路径或绝对路径，支持模板字符串
         * @see #multiPartFile(String, String, String, String)
         */
        public Builder multiPartFile(String name, String path, String contentType) {
            HeaderManager headers = new HeaderManager();
            headers.add(Header.of(CONTENT_TYPE, contentType));
            multipart.add(Part.ofFile(name, headers, path));
            return this;
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式上传文件
         * <pre><code>
         * // Part Headers
         * Content-Disposition: form-data; name="<name>"; filename="<filename>"
         * Content-Type: <contentType>
         *
         * // Example:
         * Content-Disposition: form-data; name="file"; filename="武功秘籍.pdf"
         * Content-Type: application/pdf
         * </code></pre>
         *
         * @param name        Part 名称
         * @param filename    文件名称
         * @param path        要上传的文件路径，相对路径或绝对路径，支持模板字符串
         * @param contentType Content-Type 值
         * @return 当前对象
         */
        public Builder multiPartFile(String name, String filename, String path, String contentType) {
            HeaderManager headers = Part.createPartHeaders(name, filename, contentType);
            return multiPartFile(name, headers, path);
        }

        public Builder multiPartFile(String name, HeaderManager headers, String path) {
            multipart.add(Part.ofFile(name, headers, path));
            return this;
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式发送文本数据（如果 body 包含表达式，则可能为其他类型数据）
         * <pre><code>
         * // Part Headers
         * Content-Disposition: form-data; name="<name>"
         * Content-Type: text/plain
         * </code></pre>
         * 如果 body 参数计算后为非 String 类型，且需要正确的 Content-Type Header，请使用 {@link #multiPart(String, Object, String)}。
         */
        public Builder multiPart(String name, String body) {
            HeaderManager headers = Part.createPartHeaders(name, null, TEXT_PLAIN.value());
            return multiPart(name, headers, (Object) body);
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式发送二进制数据
         * <pre><code>
         * // Part Headers
         * Content-Disposition: form-data; name="<name>"
         * Content-Type: application/octet-stream
         * </code></pre>
         */
        public Builder multiPart(String name, byte[] body) {
            return multiPart(name, body, MediaType.APPLICATION_OCTET_STREAM.value());
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式发送对象数据
         * <p>如果 body 最终类型不是 byte[]/File/String，则对象会被序列化为 JSON 字符串后发送</p>
         *
         * <pre><code>
         * // Part Headers
         * Content-Disposition: form-data; name="<name>"
         * Content-Type: application/json
         * </code></pre>
         *
         * @param body Part Body，可以是模板字符串，如 "${genAddress()}"
         * @see #multiPart(String, Object, String)
         */
        public Builder multiPart(String name, Object body) {
            HeaderManager headers = Part.createPartHeaders(name, null, MediaType.APPLICATION_JSON.value());
            return multiPart(name, headers, body);
        }

        /**
         * 请求 Body：使用 multipart/form-data 方式发送数据
         * <p>如果 body 最终类型不是 byte[]/File/String，则对象会被序列化为 JSON 字符串后发送</p>
         * <pre><code>
         * // Part Headers
         * Content-Disposition: form-data; name="<name>"
         * Content-Type: <contentType>
         * </code></pre>
         *
         * @param name        Part 名称
         * @param body        Part Body，可以是模板字符串，如 "${genAddress()}"
         * @param contentType Content-Type 值
         * @see #multiPart(String, HeaderManager, Object)
         */
        public Builder multiPart(String name, Object body, String contentType) {
            HeaderManager headers = Part.createPartHeaders(name, null, contentType);
            return multiPart(name, headers, body);
        }

        /**
         * 请求 Body：使用 MultiPart 方式发送 Body 数据
         * <p>
         * 如果请求不是 multipart/form-data 方式，而是其他 multipart 方式，如 multipart/mixed，可以使用该方法，
         * 否则推荐使用上面更方便的 multiPart 方法。
         * <a href="https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html">Multipart</a>
         *
         * @param name    Part 名称
         * @param headers Part Headers
         * @param body    Part Body
         * @return 当前对象
         */
        public Builder multiPart(String name, HeaderManager headers, Object body) {
            multipart.add(Part.of(name, headers, body));
            return this;
        }

        /**
         * 请求 Body：MultiPart Body
         *
         * @param multipart MultiPart 数据
         * @return 当前对象
         */
        public Builder multiPart(MultiPart multipart) {
            if (multipart == null) {
                return this;
            }
            this.multipart = multipart;
            return this;
        }

        // ---------------------------------------------------------------------
        // Http Response Body
        // ---------------------------------------------------------------------

        /**
         * 响应正文的保存位置
         *
         * @param path 文件路径，绝对路径或相对路径
         * @return 当前对象
         */
        public Builder download(String path) {
            this.download = path;
            return this;
        }

        // -----------------------------------------------------------

        public HttpRequest build() {
            return new HttpRequest(this);
        }

    }

}
