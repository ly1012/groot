package com.liyunx.groot.protocol.http.okhttp;

import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.protocol.http.*;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.liyunx.groot.protocol.http.model.*;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * OkHttp 实现
 */
public class OkHttpExecutor implements HttpExecutor {

    private static final OkHttpClient defaultClient = new OkHttpClient.Builder()
        // 默认连接数 5 太少，多线程跑用例时频繁创建连接会导致端口耗尽
        // TODO or 用户自定义
        .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
        // http/1.1 h2 h2c h3 协议
        // OkHttp 默认支持 HTTP2/HTTP1.1，如果可能则使用 HTTP2，否则降级使用 HTTP1.1
        // 可以强制仅使用 http/1.1，Arrays.asList(Protocol.HTTP_1_1) 。
        // 但 HTTP2 必须包含 http/1.1 作为降级协议，Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1)
        // 即不能强制仅使用 h2 协议。
        .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
        // ClearText HTTP2，不支持 HTTPS。明确服务端使用 http:// 和 HTTP2 时使用，比如一些内部服务。
        //.protocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE))
        // HTTP3/QUIC 协议，OkHttp 目前没有计划直接支持 QUIC 协议，但可以通过 Interceptor 来自己实现 QUIC 协议支持。
        //.protocols(Arrays.asList(Protocol.QUIC, Protocol.HTTP_1_1))
        // 每次请求，统计各阶段耗时和报文大小
        .eventListenerFactory(HttpStatisticEventListener.FACTORY)
        // 默认会一直重试直到成功，这里关闭自动重试
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor(ConnectionDataRecorder.INSTANCE)
        //.connectTimeout(Duration.ofSeconds(10))
        //.readTimeout(Duration.ofSeconds(8))
        .build();


    @Override
    public void execute(HttpRequest request, HttpSampleResult result) {
        HttpRequest.BodyType bodyType = request.getBodyType();

        // 构建自定义 OkHttpClient
        OkHttpClient okHttpClient = createCustomOkHttpClient(request);

        // 构建 OkHttp Request 对象
        Request okHttpRequest = createOkHttpRequest(request, bodyType);

        // 发送请求（同步阻塞）
        try (Response okHttpResponse = okHttpClient.newCall(okHttpRequest).execute()) {

            // 此时 Response Headers 数据已接收完毕

            // 获取网络请求/响应对象
            Response okHttpNetworkResponse = okHttpResponse.networkResponse();
            if (okHttpNetworkResponse != null) {
                Request okHttpNetworkRequest = okHttpNetworkResponse.request();

                // 记录实际响应数据（接收 Response Body 数据，进行响应报文处理）
                recordNetworkResponse(okHttpNetworkResponse, okHttpResponse, result, request);

                // 此时 Response Body 数据已接收完毕，请求结束

                // 记录实际连接数据
                result.setConnection(okHttpRequest.tag(HttpRealConnection.class));

                // 记录实际请求数据
                recordNetworkRequest(okHttpNetworkRequest, result, request, bodyType);
            }

            // the size smaller than listener byteCount size
            // okhttp drop some response header, like "Content-Encoding: gzip"...
            //System.out.println(response.headers().byteCount());
            //System.out.println(response.body().bytes().length);
        } catch (IOException e) {
            throw new GrootException("HTTP 请求错误，%s", e, e.getMessage());
        }

        // 记录统计数据
        result.setStat(okHttpRequest.tag(HttpStatistic.class));
    }

    private OkHttpClient createCustomOkHttpClient(HttpRequest request) {
        OkHttpClient.Builder builder = defaultClient.newBuilder();

        // HTTP Proxy
        HttpProxy httpProxy = request.getHttpServiceConfigItem().getProxy();
        if (httpProxy != null) {
            SocketAddress socketAddress = new InetSocketAddress(httpProxy.getIp(), httpProxy.getPort());
            Proxy proxy = new Proxy(Proxy.Type.HTTP, socketAddress);
            builder.proxy(proxy);
        }

        // HTTPS 强校验
        if (!request.getHttpServiceConfigItem().getVerify()) {
            builder
                .sslSocketFactory(InsecureSSL.INSECURE_SSL_SOCKET_FACTORY, InsecureSSL.INSECURE_X509_TRUST_MANAGER)
                .hostnameVerifier(InsecureSSL.INSECURE_HOSTNAME_VERIFIER);
        }

        return builder.build();
    }

    private Request createOkHttpRequest(HttpRequest request, HttpRequest.BodyType bodyType) {
        Request.Builder builder = new Request.Builder();

        // URL
        builder.url(createUrl(request));

        // Header
        HeaderManager headerManager = request.getHeaders();
        if (headerManager != null) {
            builder.headers(createHeaders(headerManager, header -> true));
        }

        // Method and Body
        builder.method(request.getMethod(), createRequestBody(request, headerManager, bodyType));

        // Sample Result Data
        builder.tag(HttpStatistic.class, new HttpStatistic());

        HttpRealConnection realConnection = new HttpRealConnection();
        realConnection.setReuse(true);        // 设置默认复用了连接，如果请求时创建了新的连接，将设置为 false
        builder.tag(HttpRealConnection.class, realConnection);

        return builder.build();
    }

    private String createUrl(HttpRequest request) {
        String url = request.getUrl();
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null)
            throw new InvalidDataException("%s 无法解析为标准 Http Url", url);

        // 无查询参数
        QueryParamManager queryParams = request.getParams();
        if (queryParams == null) {
            return url;
        }

        // 有查询参数
        HttpUrl.Builder builder = httpUrl.newBuilder();
        queryParams.forEach(queryParam -> {
            if (queryParam.isEncoded()) {
                builder.addEncodedQueryParameter(queryParam.getName(), queryParam.getValue());
            } else {
                builder.addQueryParameter(queryParam.getName(), queryParam.getValue());
            }
        });
        return builder.build().toString();
    }

    // filter：添加 Header 时的过滤器，不满足条件的 Header 将被舍弃
    private Headers createHeaders(HeaderManager headerManager, Predicate<Header> filter) {
        if (headerManager == null)
            return null;

        Headers.Builder builder = new Headers.Builder();
        headerManager.forEach(header -> {
            if (filter.test(header)) {
                // Request Header 值规范：https://www.rfc-editor.org/rfc/rfc9110.html#section-5.5
                // 一般来说，Header 值允许所有 ASCII 可见字符
                // 如果值中包含特殊字符，如中文，需要在客户端编码，服务端解码，编码和解码算法需要保持一致
                // 这里不能使用 URLEncoder.encode() 作为默认编码算法来解决中文字符问题，因为我们不知道服务端的解码算法
                // 如果值包含中文，需要用户自行处理，如 username: ${encode("中文用户名")}
                builder.addUnsafeNonAscii(header.getName(), header.getValue());
            }
        });

        return builder.build();
    }

    private RequestBody createRequestBody(HttpRequest request,
                                          HeaderManager headerManager,
                                          HttpRequest.BodyType bodyType) {
        // 获取 MediaType
        MediaType mediaType = getMediaType(headerManager);

        // 构建 RequestBody
        return switch (bodyType) {
            case JSON -> createRequestBodyByJson(request, mediaType);
            case FORM -> createRequestBodyByForm(request);
            case MULTIPART -> createRequestBodyByMultipart(request, mediaType);
            case BINARY -> createRequestBodyByBinary(request, mediaType);
            case DATA -> createRequestBodyByData(request, mediaType);
            default -> null;
        };
    }

    private MediaType getMediaType(HeaderManager headerManager) {
        MediaType mediaType = null;
        if (headerManager != null) {
            Header contentTypeHeader = headerManager.getHeader(HttpHeader.CONTENT_TYPE);
            if (contentTypeHeader != null) {
                mediaType = MediaType.get(contentTypeHeader.getValue());
            }
        }
        return mediaType;
    }

    private RequestBody createRequestBodyByJson(HttpRequest request, MediaType mediaType) {
        // 前面已处理，只会是 String 类型
        String jsonBody = (String) request.getJson();
        return RequestBody.create(jsonBody, mediaType);
    }

    private RequestBody createRequestBodyByForm(HttpRequest request) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        request.getForm().forEach(formParam -> {
            if (formParam.isEncoded()) {
                formBodyBuilder.addEncoded(formParam.getName(), formParam.getValue());
            } else {
                formBodyBuilder.add(formParam.getName(), formParam.getValue());
            }
        });
        return formBodyBuilder.build();
    }

    private RequestBody createRequestBodyByMultipart(HttpRequest request, MediaType mediaType) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        // Body 的 Content-Type
        builder.setType(mediaType);
        request.getMultipart().forEach(part -> {
            HeaderManager headerManager = part.getHeaders();

            // OkHttp 中 Part 的 Headers 不能有 Content-Type 请求头，
            // Part 的 Content-Type，在 RequestBody 中指定
            Headers headers = createHeaders(
                headerManager,
                header -> !header.getName().equalsIgnoreCase(HttpHeader.CONTENT_TYPE.value()));

            // Part 的 Content-Type
            MediaType mediaTypeInPart = getMediaType(headerManager);
            RequestBody requestBody = createRequestBodyByData(part.getBody(), mediaTypeInPart);
            builder.addPart(headers, requestBody);
        });
        return builder.build();
    }

    private RequestBody createRequestBodyByBinary(HttpRequest request, MediaType mediaType) {
        Object binaryBody = request.getBinary();
        // 前面已经校验必须是这两种类型，见 HttpRequest.check 方法
        if (binaryBody instanceof byte[]) {
            return RequestBody.create((byte[]) binaryBody, mediaType);
        }
        if (binaryBody instanceof File) {
            return RequestBody.create((File) binaryBody, mediaType);
        }
        return null;
    }

    private RequestBody createRequestBodyByData(HttpRequest request, MediaType mediaType) {
        return createRequestBodyByData(request.getData(), mediaType);
    }

    private RequestBody createRequestBodyByData(Object dataBody, MediaType mediaType) {
        // 前面已处理，只会是这三种类型
        if (dataBody instanceof byte[]) {
            return RequestBody.create((byte[]) dataBody, mediaType);
        }
        if (dataBody instanceof File) {
            return RequestBody.create((File) dataBody, mediaType);
        }
        if (dataBody instanceof String) {
            return RequestBody.create((String) dataBody, mediaType);
        }
        return null;
    }

    private void recordNetworkRequest(Request okHttpNetworkRequest,
                                      HttpSampleResult result,
                                      HttpRequest request,
                                      HttpRequest.BodyType bodyType) {
        HttpRealRequest realRequest = new HttpRealRequest();

        // == 请求行 ==
        // method
        realRequest.setMethod(okHttpNetworkRequest.method());
        // url
        HttpUrl url = okHttpNetworkRequest.url();
        realRequest.setUrl(url.toString());
        QueryParamManager queryParamManager = new QueryParamManager();
        for (int i = 0; i < url.querySize(); i++) {
            queryParamManager.add(new QueryParam(url.queryParameterName(i), url.queryParameterValue(i)));
        }
        realRequest.setParams(queryParamManager);
        // protocol
        realRequest.setProtocol(result.getConnection().getProtocol());

        // == 请求 Header ==
        HeaderManager headerManager = new HeaderManager();
        okHttpNetworkRequest.headers().forEach(header -> {
            headerManager.add(new Header(header.getFirst(), header.getSecond()));
        });
        realRequest.setHeaders(headerManager);
        realRequest.setCookies(headerManager.getCookies());

        // == 请求 Body ==
        // 判断请求 Body 是否是文件
        File bodyFile = getRequestBodyFile(request, bodyType);
        if (bodyFile == null) {
            realRequest.setFile(false);
            // TODO multipart 请求需要额外的字段补充记录，提供方便访问的 API
            if (HttpRequest.BodyType.MULTIPART.equals(bodyType)) {
                MultiPart multiPart = request.getMultipart();
                MultipartBody realMultiPart = (MultipartBody) okHttpNetworkRequest.body();
                if (realMultiPart == null) {
                    throw new IllegalStateException("multipart 请求体为空");
                }
                if (multiPart.size() != realMultiPart.size()) {
                    throw new IllegalStateException("multipart 请求体大小不一致");
                }
                // 请求体可能包含文件（使用地址代替文件内容）
                realRequest.setBody(multipartRequestBodyToString(multiPart, realMultiPart));
            } else {
                realRequest.setBody(requestBodyToString(okHttpNetworkRequest.body()));
            }
        } else {
            realRequest.setFile(true);
            realRequest.setBodyFile(bodyFile);
        }

        result.setRequest(realRequest);
    }

    // multiPart：Groot 预请求 Body 表示（准备发送），用于判断 Part.Body 是否是文件
    // body：OKHttp 实际请求 Body 表示（实际发送）
    private String multipartRequestBodyToString(MultiPart multiPart, MultipartBody realMultiPart) {
        StringBuilder bodyString = new StringBuilder();
        String boundary = realMultiPart.boundary();
        for (int i = 0; i < multiPart.size(); i++) {
            Part part = multiPart.get(i);
            MultipartBody.Part realPart = realMultiPart.part(i);
            Headers headers = realPart.headers();
            RequestBody body = realPart.body();

            bodyString.append("--").append(boundary).append("\n");

            // headers
            if (headers != null) {
                for (int h = 0; h < headers.size(); h++) {
                    bodyString
                        .append(headers.name(h))
                        .append(": ")
                        .append(headers.value(h))
                        .append("\n");
                }
            }

            MediaType contentType = body.contentType();
            if (contentType != null) {
                bodyString.append("Content-Type: ").append(contentType).append("\n");
            }

            long contentLength = 0;
            try {
                contentLength = body.contentLength();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (contentLength != -1L) {
                bodyString.append("Content-Length: ").append(contentLength).append("\n");
            }

            // body
            bodyString.append("\n");
            if (part.getBody() instanceof File file) {
                bodyString.append("<").append(file.getAbsolutePath()).append(">");
            } else {
                bodyString.append(requestBodyToString(body));
            }
            bodyString.append("\n");
        }
        bodyString
            .append("--")
            .append(boundary)
            .append("--");
        return bodyString.toString();
    }

    private File getRequestBodyFile(HttpRequest request, HttpRequest.BodyType bodyType) {
        switch (bodyType) {
            case BINARY:
                if (request.getBinary() instanceof File) {
                    return (File) request.getBinary();
                }
                break;

            case DATA:
                if (request.getData() instanceof File) {
                    return (File) request.getData();
                }
                break;
        }
        return null;
    }

    private String requestBodyToString(RequestBody body) {
        if (body == null) {
            return null;
        }

        Buffer buffer = new Buffer();
        try {
            body.writeTo(buffer);
        } catch (IOException e) {
            throw new GrootException("OkHttp Request Body 转换 String 失败，%s", e, e.getMessage());
        }
        return buffer.readUtf8();
    }

    private void recordNetworkResponse(Response okHttpNetworkResponse,
                                       Response okHttpResponse,
                                       HttpSampleResult result,
                                       HttpRequest request) {
        // TODO 重定向的情况下，记录每次重定向数据？
        //okHttpResponse.priorResponse();

        HttpRealResponse realResponse = new HttpRealResponse();
        result.setResponse(realResponse);

        // == 响应 Body ==
        // body() always returns null on responses returned from cacheResponse, networkResponse, and priorResponse.
        // use response directly.
        ResponseBody responseBody = okHttpResponse.body();
        if (responseBody == null) {
            return;
        }
        // TODO multipart 响应可能包含文件，需要优化下，使用单独的字段记录
        // 暂时不做，需要再做，需求过于小众
        //MultipartReader multipartReader = new MultipartReader(Objects.requireNonNull(response.body()));
        //multipartReader.nextPart();
        String download = request.getDownload();
        if (download == null) {      // 不保存响应到指定文件
            try {
                String bodyAsString = responseBody.string();
                realResponse.setFile(false);
                realResponse.setBody(bodyAsString);
            } catch (IOException e) {
                throw new GrootException("读取响应 Body 出错，%s", e, e.getMessage());
            }
        } else {                    // 保存响应到指定文件
            File downloadFile = FileUtil.createFileOrDirectory(download, false, true);
            try (BufferedSink sink = Okio.buffer(Okio.sink(downloadFile))) {
                sink.writeAll(responseBody.source());
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                throw new GrootException("读取响应 Body 出错，%s", e, e.getMessage());
            }
            realResponse.setFile(true);
            realResponse.setBodyFile(downloadFile);
        }

        // == 状态行 ==
        // protocol
        realResponse.setProtocol(okHttpNetworkResponse.protocol().toString());
        // 状态码
        realResponse.setStatus(okHttpNetworkResponse.code());
        // 状态信息
        realResponse.setMessage(okHttpNetworkResponse.message());

        // == 响应 Header ==
        HeaderManager headerManager = new HeaderManager();
        okHttpNetworkResponse.headers().forEach(header -> {
            headerManager.add(new Header(header.getFirst(), header.getSecond()));
        });
        realResponse.setHeaders(headerManager);


    }

}
