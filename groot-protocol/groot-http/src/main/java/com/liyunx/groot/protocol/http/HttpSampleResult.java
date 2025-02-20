package com.liyunx.groot.protocol.http;

import com.liyunx.groot.testelement.sampler.SampleResult;

/**
 * HttpSampler 执行结果
 */
public class HttpSampleResult extends SampleResult<HttpSampleResult> {

    private HttpRealConnection connection;

    private HttpStatistic stat;

    // == Getter/Setter ==

    public HttpRealConnection getConnection() {
        return connection;
    }

    public void setConnection(HttpRealConnection connection) {
        this.connection = connection;
    }

    public HttpStatistic getStat() {
        return stat;
    }

    public void setStat(HttpStatistic stat) {
        this.stat = stat;
    }

    public HttpRealRequest getRequest() {
        return (HttpRealRequest) request;
    }

    public void setRequest(HttpRealRequest request) {
        this.request = request;
    }

    public HttpRealResponse getResponse() {
        return (HttpRealResponse) response;
    }

    public void setResponse(HttpRealResponse response) {
        this.response = response;
    }

}
