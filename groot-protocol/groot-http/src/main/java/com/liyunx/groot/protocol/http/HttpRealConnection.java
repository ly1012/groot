package com.liyunx.groot.protocol.http;

/**
 * Http 请求实际连接数据
 */
public class HttpRealConnection {

    // == 五元组信息 ==

    // 本地地址，使用 local 前缀区分 ==

    /**
     * 本地 IP，如 192.168.1.20
     */
    private String localIp;

    /**
     * 本地连接端口，如 14384
     */
    private int localPort;

    // 远程地址，无前缀，默认表示远程（目标）地址

    /**
     * 远程 IP，如 114.118.12.69
     */
    private String ip;

    /**
     * 远程端口，如 443
     */
    private int port;

    // 协议

    /**
     * 协议版本，如 http/1.1
     */
    private String protocol;

    // == 加密信息 ==

    /**
     * TLS 版本，如 TLSv1.2
     */
    private String tlsVersion;

    /**
     * 加密套件，如 TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
     */
    private String cipher;

    // == 其他信息 ==

    /**
     * 本次请求是否复用了连接，true 表示复用
     */
    private boolean reuse;


    // == Getter/Setter ==


    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

    public void setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean getReuse() {
        return reuse;
    }

    public void setReuse(boolean reuse) {
        this.reuse = reuse;
    }
}
