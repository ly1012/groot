package com.liyunx.groot.protocol.http;

import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Http 请求统计信息，包括耗时信息与报文大小
 */
public class HttpStatistic {

    private static final Logger log = LoggerFactory.getLogger(HttpStatistic.class);

    // == 一些无关紧要的间隔时间，一般来说很短，除非 HttpClient 首次请求 ==
    private double callStartToDnsStart;
    private double dnsEndToConnectStart;
    private double connectEndToRequestHeadersStart;
    private double requestHeadersEndToRequestBodyStart;
    private double responseHeadersEndToResponseBodyStart;
    private double responseBodyEndToCallEnd;

    // 纳秒或微秒，使用时不太方便，因此使用习惯的毫秒，但毫秒精度可能不够，
    // 比如 DNS 查找时间为 0.43ms，如果使用 long 存储时，四舍五入，直接为 0，丢失数据，
    // 因此使用 double + ms

    // == DNS 查找耗时 ==

    /**
     * DNS 查找耗时，单位：ms
     */
    private double dnsLookupTime;

    // == 连接耗时 ==

    /**
     * TCP 连接耗时，单位：ms
     */
    private double tcpHandshakeTime;
    /**
     * SSL/TLS 连接耗时，单位：ms
     */
    private double sslHandshakeTime;
    /**
     * 连接总耗时，单位：ms
     */
    private double connectTime;

    // == 请求上行耗时 ==

    /**
     * 请求头传输耗时，单位：ms
     */
    private double requestHeadersTransferTime;
    /**
     * 请求体传输耗时，单位：ms
     */
    private double requestBodyTransferTime;
    /**
     * 请求传输耗时，单位：ms
     */
    private double requestTransferTime;

    // == 服务处理耗时 ==

    /**
     * 服务器处理数据耗时，单位：ms
     */
    private double serviceProcessTime;

    // == 响应下行耗时 ==

    /**
     * 响应头传输耗时，单位：ms
     */
    private double responseHeadersTransferTime;
    /**
     * 响应体传输耗时，单位：ms
     */
    private double responseBodyTransferTime;
    /**
     * 响应传输耗时，单位：ms
     */
    private double responseTransferTime;

    // == 总耗时 ==

    /**
     * 本次请求调用总耗时，包括请求无关的代码耗时，如拦截器处理等等，单位：ms
     */
    private double time;

    // == 请求大小 ==

    /**
     * 请求头大小，单位：byte/B
     */
    private long requestHeadersSize;
    /**
     * 请求体大小，单位：byte/B
     */
    private long requestBodySize;
    /**
     * 请求头与请求体的总大小，单位：byte/B
     */
    private long requestSize;

    // == 响应大小 ==

    /**
     * 响应头大小，单位：byte/B
     */
    private long responseHeadersSize;
    /**
     * 响应体大小，单位：byte/B
     */
    private long responseBodySize;
    /**
     * 响应头与响应体的总大小，单位：byte/B
     */
    private long responseSize;

    private static final String BORDER = "|";
    private static final String SINGLE_EMPTY = " ";
    private static final String SINGLE_FILL = "\u25A0";
    private static final byte STAT_NUM = 10;

    public void prettyPrint() {
        prettyPrintTime();
        prettyPrintSize();
    }

    public void prettyPrintTime() {
        // 计算百分比
        int[] percent = new int[STAT_NUM];
        percent[0] = percent(callStartToDnsStart, time);
        percent[1] = percent(dnsLookupTime, time);
        percent[2] = percent(dnsEndToConnectStart, time);
        percent[3] = percent(tcpHandshakeTime, time);
        percent[4] = percent(sslHandshakeTime, time);
        percent[5] = percent(connectEndToRequestHeadersStart, time);
        percent[6] = percent(requestTransferTime, time);
        percent[7] = percent(serviceProcessTime, time);
        percent[8] = percent(responseTransferTime, time);
        percent[9] = percent(responseBodyEndToCallEnd, time);

        // 误差配平（百分向上取整带来的误差）
        int percentSum = 0;
        for (int i = 0; i < STAT_NUM; i++) {
            percentSum += percent[i];
        }
        int diff = percentSum - 100;    // [0, 10] 之间
        if (diff > STAT_NUM || diff < 0) {
            throw new InvalidDataException("请求耗时数据不合法，各阶段耗时之和不等于总耗时，data: %s", this);
        }
        boolean[] locked = new boolean[STAT_NUM];     //保护标记，被保护的元素不会被砍
        boolean[] decreased = new boolean[STAT_NUM];  //被砍标记
        while (diff > 0) {
            // 选择合适的位置砍一刀
            // 有值的位置，至少要占一格；优先砍向值最大的位置；尽量将砍刀挥向不同的位置

            // 查找符合要求的索引
            byte index = findMaxValueIndex(percent, locked, decreased);
            // 一轮砍刀结束，开始下一轮，重新查找一次
            if (index == -1) {
                Arrays.fill(decreased, false);
                index = findMaxValueIndex(percent, locked, decreased);
            }
            // 砍一刀
            percent[index]--;
            decreased[index] = true;
            diff--;
        }

        // 构建耗时分析日志
        StringBuilder builder = new StringBuilder();
        int sum = 0;
        builder.append("HTTP 请求耗时：");
        // 各阶段耗时
        itemLine(builder, dnsLookupTime == 0 ? "DNS Lookup(Skip)" : "DNS Lookup",
            sum += percent[0], percent[1], dnsLookupTime);
        itemLine(builder, tcpHandshakeTime == 0 ? "TCP Handshake(Skip)" : "TCP Handshake",
            sum += percent[1] + percent[2], percent[3], tcpHandshakeTime);
        itemLine(builder, sslHandshakeTime == 0 ? "SSL Handshake(Skip)" : "SSL Handshake",
            sum += percent[3], percent[4], sslHandshakeTime);
        itemLine(builder, "Request Transfer",
            sum += percent[4] + percent[5], percent[6], requestTransferTime);
        itemLine(builder, "Server Process",
            sum += percent[6], percent[7], serviceProcessTime);
        itemLine(builder, "Response Transfer",
            sum + percent[7], percent[8], responseTransferTime);
        // 请求调用耗时
        builder.append("\n").append(StringUtil.rightPad("Total", 122, SINGLE_EMPTY));
        endItem(builder, time);

        log.info(builder.toString());
    }

    //Request Size     15.96 KB /    96 B
    //Header              96 B  /    96 B
    //Body             10.00 KB /     0 B
    //
    //Response Size   15.96 KB /         17578 B
    //Header              2 B  /            18 B
    //Body            13.96 KB / 1612882254340 B
    public void prettyPrintSize() {
        // 计算 Hummable Value，如 KB、MB、GB
        String[] hs = new String[6];
        hs[0] = hummableValue(requestSize);
        hs[1] = hummableValue(requestHeadersSize);
        hs[2] = hummableValue(requestBodySize);
        hs[3] = hummableValue(responseSize);
        hs[4] = hummableValue(responseHeadersSize);
        hs[5] = hummableValue(responseBodySize);

        int maxHummableLength = 0;
        for (int i = 0; i < 6; i++) {
            int length = hs[i].length();
            if (length > maxHummableLength) {
                maxHummableLength = length;
            }
        }
        maxHummableLength += 2;

        int maxLongLength = Math.max(Long.toString(requestSize).length(), Long.toString(responseSize).length());

        StringBuilder builder = new StringBuilder();
        builder.append("HTTP 请求报文大小：");
        sizeLine(builder, "Request Size", hs[0], requestSize, maxHummableLength, maxLongLength);
        sizeLine(builder, "Header", hs[1], requestHeadersSize, maxHummableLength, maxLongLength);
        sizeLine(builder, "Body", hs[2], requestBodySize, maxHummableLength, maxLongLength);
        builder.append("\n");
        sizeLine(builder, "Response Size", hs[3], responseSize, maxHummableLength, maxLongLength);
        sizeLine(builder, "Header", hs[4], responseHeadersSize, maxHummableLength, maxLongLength);
        sizeLine(builder, "Body", hs[5], responseBodySize, maxHummableLength, maxLongLength);

        log.info(builder.toString());
    }

    private String hummableValue(long size) {
        if (size <= 1024) {                        // B
            return size + " B ";
        } else if (size <= 1024 * 1024) {          // KB
            return String.format("%.2f KB", size / 1024.0);
        } else if (size <= 1024 * 1024 * 1024) {   // MB
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {                                   // GB
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    private void sizeLine(StringBuilder builder,
                          String sizeName, String hummableValue, long value, int maxHummableLength, int maxLongLength) {
        builder
            .append("\n")
            .append(StringUtil.rightPad(sizeName, 15, SINGLE_EMPTY))
            .append(StringUtil.leftPad(hummableValue, maxHummableLength, SINGLE_EMPTY))
            .append(" / ")
            .append(StringUtil.leftPad(Long.toString(value), maxLongLength, SINGLE_EMPTY))
            .append(" B");
    }

    private byte findMaxValueIndex(int[] percent, boolean[] locked, boolean[] decreased) {
        byte index = -1;
        int max = 0;
        for (byte i = 0; i < STAT_NUM; i++) {
            if (locked[i] || decreased[i])
                continue;

            int current = percent[i];
            if (current == 1) {
                locked[i] = true;
            } else if (current > max) {
                max = current;
                index = i;
            }
        }
        return index;
    }

    // 返回 [0, 100] 之间的值
    private int percent(double numerator, double denominator) {
        double percent = numerator / denominator;
        return (int) Math.ceil(percent * 100);       // 向上取整，防止时间过短无法显示
    }

    private void itemLine(StringBuilder builder, String itemName, int left, int length, double time) {
        builder.append("\n");
        statItem(builder, itemName);
        builder.append(BORDER);
        progress(builder, left, length);
        builder.append(BORDER);
        endItem(builder, time);
    }

    private void statItem(StringBuilder builder, String itemName) {
        builder
            .append(StringUtil.rightPad(itemName, 20, SINGLE_EMPTY));

    }

    private void progress(StringBuilder builder, int left, int length) {
        builder
            .append(StringUtil.repeat(SINGLE_EMPTY, left))
            .append(StringUtil.repeat(SINGLE_FILL, length))
            .append(StringUtil.repeat(SINGLE_EMPTY, 100 - left - length));
    }

    private void endItem(StringBuilder builder, double time) {
        builder
            .append(StringUtil.leftPad(String.format("%.2f", time), 10, SINGLE_EMPTY))
            .append(" ms");
    }


    // Getter/Setter

    public double getDnsLookupTime() {
        return dnsLookupTime;
    }

    public void setDnsLookupTime(double dnsLookupTime) {
        this.dnsLookupTime = dnsLookupTime;
    }

    public double getTcpHandshakeTime() {
        return tcpHandshakeTime;
    }

    public void setTcpHandshakeTime(double tcpHandshakeTime) {
        this.tcpHandshakeTime = tcpHandshakeTime;
    }

    public double getSslHandshakeTime() {
        return sslHandshakeTime;
    }

    public void setSslHandshakeTime(double sslHandshakeTime) {
        this.sslHandshakeTime = sslHandshakeTime;
    }

    public double getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(double connectTime) {
        this.connectTime = connectTime;
    }

    public double getRequestHeadersTransferTime() {
        return requestHeadersTransferTime;
    }

    public void setRequestHeadersTransferTime(double requestHeadersTransferTime) {
        this.requestHeadersTransferTime = requestHeadersTransferTime;
    }

    public double getRequestBodyTransferTime() {
        return requestBodyTransferTime;
    }

    public void setRequestBodyTransferTime(double requestBodyTransferTime) {
        this.requestBodyTransferTime = requestBodyTransferTime;
    }

    public double getRequestTransferTime() {
        return requestTransferTime;
    }

    public void setRequestTransferTime(double requestTransferTime) {
        this.requestTransferTime = requestTransferTime;
    }

    public double getServiceProcessTime() {
        return serviceProcessTime;
    }

    public void setServiceProcessTime(double serviceProcessTime) {
        this.serviceProcessTime = serviceProcessTime;
    }

    public double getResponseHeadersTransferTime() {
        return responseHeadersTransferTime;
    }

    public void setResponseHeadersTransferTime(double responseHeadersTransferTime) {
        this.responseHeadersTransferTime = responseHeadersTransferTime;
    }

    public double getResponseBodyTransferTime() {
        return responseBodyTransferTime;
    }

    public void setResponseBodyTransferTime(double responseBodyTransferTime) {
        this.responseBodyTransferTime = responseBodyTransferTime;
    }

    public double getResponseTransferTime() {
        return responseTransferTime;
    }

    public void setResponseTransferTime(double responseTransferTime) {
        this.responseTransferTime = responseTransferTime;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public long getRequestHeadersSize() {
        return requestHeadersSize;
    }

    public void setRequestHeadersSize(long requestHeadersSize) {
        this.requestHeadersSize = requestHeadersSize;
    }

    public long getRequestBodySize() {
        return requestBodySize;
    }

    public void setRequestBodySize(long requestBodySize) {
        this.requestBodySize = requestBodySize;
    }

    public long getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(long requestSize) {
        this.requestSize = requestSize;
    }

    public long getResponseHeadersSize() {
        return responseHeadersSize;
    }

    public void setResponseHeadersSize(long responseHeadersSize) {
        this.responseHeadersSize = responseHeadersSize;
    }

    public long getResponseBodySize() {
        return responseBodySize;
    }

    public void setResponseBodySize(long responseBodySize) {
        this.responseBodySize = responseBodySize;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    public double getCallStartToDnsStart() {
        return callStartToDnsStart;
    }

    public void setCallStartToDnsStart(double callStartToDnsStart) {
        this.callStartToDnsStart = callStartToDnsStart;
    }

    public double getDnsEndToConnectStart() {
        return dnsEndToConnectStart;
    }

    public void setDnsEndToConnectStart(double dnsEndToConnectStart) {
        this.dnsEndToConnectStart = dnsEndToConnectStart;
    }

    public double getConnectEndToRequestHeadersStart() {
        return connectEndToRequestHeadersStart;
    }

    public void setConnectEndToRequestHeadersStart(double connectEndToRequestHeadersStart) {
        this.connectEndToRequestHeadersStart = connectEndToRequestHeadersStart;
    }

    public double getRequestHeadersEndToRequestBodyStart() {
        return requestHeadersEndToRequestBodyStart;
    }

    public void setRequestHeadersEndToRequestBodyStart(double requestHeadersEndToRequestBodyStart) {
        this.requestHeadersEndToRequestBodyStart = requestHeadersEndToRequestBodyStart;
    }

    public double getResponseHeadersEndToResponseBodyStart() {
        return responseHeadersEndToResponseBodyStart;
    }

    public void setResponseHeadersEndToResponseBodyStart(double responseHeadersEndToResponseBodyStart) {
        this.responseHeadersEndToResponseBodyStart = responseHeadersEndToResponseBodyStart;
    }

    public double getResponseBodyEndToCallEnd() {
        return responseBodyEndToCallEnd;
    }

    public void setResponseBodyEndToCallEnd(double responseBodyEndToCallEnd) {
        this.responseBodyEndToCallEnd = responseBodyEndToCallEnd;
    }

    @Override
    public String toString() {
        return "HttpStatistic{" +
            "callStartToDnsStart=" + callStartToDnsStart +
            ", dnsEndToConnectStart=" + dnsEndToConnectStart +
            ", connectEndToRequestHeadersStart=" + connectEndToRequestHeadersStart +
            ", requestHeadersEndToRequestBodyStart=" + requestHeadersEndToRequestBodyStart +
            ", responseHeadersEndToResponseBodyStart=" + responseHeadersEndToResponseBodyStart +
            ", responseBodyEndToCallEnd=" + responseBodyEndToCallEnd +
            ", dnsLookupTime=" + dnsLookupTime +
            ", tcpHandshakeTime=" + tcpHandshakeTime +
            ", sslHandshakeTime=" + sslHandshakeTime +
            ", connectTime=" + connectTime +
            ", requestHeadersTransferTime=" + requestHeadersTransferTime +
            ", requestBodyTransferTime=" + requestBodyTransferTime +
            ", requestTransferTime=" + requestTransferTime +
            ", serviceProcessTime=" + serviceProcessTime +
            ", responseHeadersTransferTime=" + responseHeadersTransferTime +
            ", responseBodyTransferTime=" + responseBodyTransferTime +
            ", responseTransferTime=" + responseTransferTime +
            ", time=" + time +
            ", requestHeadersSize=" + requestHeadersSize +
            ", requestBodySize=" + requestBodySize +
            ", requestSize=" + requestSize +
            ", responseHeadersSize=" + responseHeadersSize +
            ", responseBodySize=" + responseBodySize +
            ", responseSize=" + responseSize +
            '}';
    }
}
