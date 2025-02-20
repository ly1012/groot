package com.liyunx.groot.protocol.http.okhttp;

import com.liyunx.groot.exception.GrootException;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * 忽视 SSL 证书校验
 */
public class InsecureSSL {

    public static final X509TrustManager INSECURE_X509_TRUST_MANAGER = insecureX509TrustManager();
    public static final SSLSocketFactory INSECURE_SSL_SOCKET_FACTORY = insecureSSLSocketFactory();
    public static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = insecureHostnameVerifier();

    public static X509TrustManager insecureX509TrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };
    }

    public static SSLSocketFactory insecureSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{INSECURE_X509_TRUST_MANAGER}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new GrootException(e);
        }

    }

    public static HostnameVerifier insecureHostnameVerifier() {
        return (arg0, arg1) -> true;
    }

}
