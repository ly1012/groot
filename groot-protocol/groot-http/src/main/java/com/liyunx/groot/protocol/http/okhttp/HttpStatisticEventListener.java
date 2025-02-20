package com.liyunx.groot.protocol.http.okhttp;

import com.liyunx.groot.protocol.http.HttpRealConnection;
import com.liyunx.groot.protocol.http.HttpStatistic;
import okhttp3.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * 统计 Http 请求的各阶段耗时以及请求响应大小
 */
public class HttpStatisticEventListener extends EventListener {

    /**
     * TimeEventListener 工厂类，用于创建 TimeEventListener 实例。
     */
    public static final EventListener.Factory FACTORY = call -> {
        HttpStatistic stat = call.request().tag(HttpStatistic.class);
        return stat != null ? new HttpStatisticEventListener(stat) : EventListener.NONE;
    };

    /**
     * Http 请求统计信息
     */
    private final HttpStatistic stat;

    public HttpStatisticEventListener(HttpStatistic httpStatistic) {
        this.stat = httpStatistic;
    }

    // == 各阶段起止时间 ==

    private long callStartTime;
    private long dnsStartTime;
    private long dnsEndTime;
    private long connectStartTime;
    private long secureConnectStartTime;
    private long secureConnectEndTime;
    private long connectEndTime;
    private long requestHeadersStartTime;
    private long requestHeadersEndTime;
    private long requestBodyStartTime;
    private long requestBodyEndTime;
    private long responseHeadersStartTime;
    private long responseHeadersEndTime;
    private long responseBodyStartTime;
    private long responseBodyEndTime;
    private long callEndTime;

    // == 各阶段报文大小 ==

    private long requestHeadersSize;
    private long requestBodySize;
    private long responseHeadersSize;
    private long responseBodySize;


    @Override
    public void callStart(Call call) {
        callStartTime = time();
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        dnsStartTime = time();
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        dnsEndTime = time();
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        connectStartTime = time();

        HttpRealConnection realConnection = call.request().tag(HttpRealConnection.class);
        if (realConnection != null) {
            realConnection.setReuse(false);
        }
    }

    @Override
    public void secureConnectStart(Call call) {
        secureConnectStartTime = time();
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        secureConnectEndTime = time();
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        long time = time();

        if (secureConnectStartTime > secureConnectEndTime)
            secureConnectEndTime = time;

        if (connectStartTime > connectEndTime)
            connectEndTime = time;
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {

        connectEndTime = time();
    }

    @Override
    public void requestHeadersStart(Call call) {
        requestHeadersStartTime = time();
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        requestHeadersEndTime = time();
        requestHeadersSize = request.headers().byteCount();
    }

    @Override
    public void requestBodyStart(Call call) {
        requestBodyStartTime = time();
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        requestBodyEndTime = time();
        requestBodySize = byteCount;
    }

    @Override
    public void requestFailed(Call call, IOException ioe) {
        if (requestHeadersStartTime > requestHeadersEndTime)
            requestHeadersEndTime = time();
        if (requestBodyStartTime > requestBodyEndTime)
            requestBodyEndTime = time();
    }

    @Override
    public void responseHeadersStart(Call call) {
        responseHeadersStartTime = time();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        responseHeadersEndTime = time();
        responseHeadersSize = response.headers().byteCount();
    }

    @Override
    public void responseBodyStart(Call call) {
        responseBodyStartTime = time();
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        responseBodyEndTime = time();
        responseBodySize = byteCount;
    }

    @Override
    public void responseFailed(Call call, IOException ioe) {
        if (responseHeadersStartTime > responseHeadersEndTime)
            responseHeadersEndTime = time();
        if (responseBodyStartTime > responseBodyEndTime)
            responseBodyEndTime = time();
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        callEndTime = time();
        calculate();
    }

    @Override
    public void callEnd(Call call) {
        callEndTime = time();
        calculate();
    }

    private void calculate() {
        // 补全缺失数据，方便运算，从后往前补全
        if (responseBodyStartTime == 0)
            responseBodyStartTime = responseBodyEndTime = callEndTime;

        if (responseHeadersStartTime == 0)
            responseHeadersStartTime = responseHeadersEndTime = responseBodyStartTime;

        if (requestBodyStartTime == 0)
            requestBodyStartTime = requestBodyEndTime = requestHeadersEndTime;

        // 要求至少需要有 Request Header，以作为没有 Request Body 时的补全数据
        // 不能以 responseHeadersStartTime 作为补全数据，否则既不合理，也无法统计服务处理时间

        if (connectStartTime == 0)
            connectStartTime = secureConnectStartTime = secureConnectEndTime = connectEndTime = requestHeadersStartTime;

        if (secureConnectStartTime == 0)
            secureConnectStartTime = secureConnectEndTime = connectEndTime;

        if (dnsStartTime == 0)
            dnsStartTime = dnsEndTime = connectStartTime;

        // 计算各阶段时间

        stat.setCallStartToDnsStart(elapseTime(callStartTime, dnsStartTime));

        stat.setDnsLookupTime(elapseTime(dnsStartTime, dnsEndTime));

        stat.setDnsEndToConnectStart(elapseTime(dnsEndTime, connectStartTime));

        double sslHandshakeTime = elapseTime(secureConnectStartTime, secureConnectEndTime);
        double connectTime = elapseTime(connectStartTime, connectEndTime);
        // 误差配平
        // 这里没有采用 secureConnectStartTime - connectStartTime 来计算 TCP 连接时间，
        // 因为 connectEndTime - secureConnectEndTime 的误差在 0.01ms，影响很小，忽略不计，
        // 所以采用了取巧的 连接总时间 - SSL 连接时间 来表示，这样 TcpHandshakeTime + SslHandshakeTime = ConnectTime。
        stat.setTcpHandshakeTime(diff(connectTime, sslHandshakeTime));
        stat.setSslHandshakeTime(sslHandshakeTime);
        stat.setConnectTime(connectTime);

        stat.setConnectEndToRequestHeadersStart(elapseTime(connectEndTime, requestHeadersStartTime));

        double requestHeadersTransferTime = elapseTime(requestHeadersStartTime, requestHeadersEndTime);
        double requestBodyTransferTime = elapseTime(requestBodyStartTime, requestBodyEndTime);
        double requestTransferTime = elapseTime(requestHeadersStartTime, requestBodyEndTime);
        stat.setRequestHeadersTransferTime(requestHeadersTransferTime);
        stat.setRequestHeadersSize(requestHeadersSize);
        // 误差配平
        // requestHeadersTransferTime + requestHeadersEndToRequestBodyStart + requestBodyTransferTime = requestTransferTime
        stat.setRequestHeadersEndToRequestBodyStart(
            diff(requestTransferTime, sum(requestHeadersTransferTime, requestBodyTransferTime)));
        stat.setRequestBodyTransferTime(requestBodyTransferTime);
        stat.setRequestBodySize(requestBodySize);
        stat.setRequestTransferTime(requestTransferTime);
        stat.setRequestSize(requestHeadersSize + requestBodySize);

        stat.setServiceProcessTime(elapseTime(requestBodyEndTime, responseHeadersStartTime));

        double responseHeadersTransferTime = elapseTime(responseHeadersStartTime, responseHeadersEndTime);
        double responseBodyTransferTime = elapseTime(responseBodyStartTime, responseBodyEndTime);
        double responseTransferTime = elapseTime(responseHeadersStartTime, responseBodyEndTime);
        stat.setResponseHeadersTransferTime(responseHeadersTransferTime);
        stat.setResponseHeadersSize(responseHeadersSize);
        // 误差配平
        // responseHeadersTransferTime + responseHeadersEndToResponseBodyStart + responseBodyTransferTime = responseTransferTime
        stat.setResponseHeadersEndToResponseBodyStart(
            diff(responseTransferTime, sum(responseHeadersTransferTime, responseBodyTransferTime)));
        stat.setResponseBodyTransferTime(responseBodyTransferTime);
        stat.setResponseBodySize(responseBodySize);
        stat.setResponseTransferTime(responseTransferTime);
        stat.setResponseSize(responseHeadersSize + responseBodySize);

        double time = elapseTime(callStartTime, callEndTime);
        // 误差配平
        stat.setResponseBodyEndToCallEnd(
            diff(time, sum(
                stat.getCallStartToDnsStart(),
                stat.getDnsLookupTime(),
                stat.getDnsEndToConnectStart(),
                stat.getConnectTime(),
                stat.getConnectEndToRequestHeadersStart(),
                stat.getRequestTransferTime(),
                stat.getServiceProcessTime(),
                stat.getResponseTransferTime())));

        stat.setTime(time);
    }

    // 返回当前时间标记值，注意不是当前时间戳，仅可用于计算时间差
    private long time() {
        return System.nanoTime();
    }

    // 计算两个时间点的时间差，单位 ms，保留两位小数（四舍五入）
    private double elapseTime(long startTime, long endTime) {
        // 纳秒时间差
        BigDecimal elapseTime = BigDecimal.valueOf(endTime - startTime);
        // 转换成毫秒时间差
        BigDecimal bigDecimal = elapseTime.divide(BigDecimal.valueOf(1e6d), 2, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    // 计算两个时间差的和，保留两位小数（四舍五入）
    private double sum(double x, double y) {
        BigDecimal _x = BigDecimal.valueOf(x);
        BigDecimal _y = BigDecimal.valueOf(y);
        return _x.add(_y)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private double sum(double... data) {
        BigDecimal sum = new BigDecimal("0");
        for (double d : data) {
            sum = sum.add(BigDecimal.valueOf(d));
        }
        return sum.doubleValue();
    }

    // 计算两个时间差的差，保留两位小数（四舍五入）
    private double diff(double bigger, double smaller) {
        BigDecimal _x = BigDecimal.valueOf(bigger);
        BigDecimal _y = BigDecimal.valueOf(smaller);
        return _x.subtract(_y)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

}
