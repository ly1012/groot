package com.liyunx.groot.protocol.http.okhttp;

import com.liyunx.groot.protocol.http.HttpRealConnection;
import okhttp3.*;

import java.io.IOException;
import java.net.Socket;

/**
 * 网络拦截器：记录请求连接数据
 */
public class ConnectionDataRecorder implements Interceptor {

    public static final ConnectionDataRecorder INSTANCE = new ConnectionDataRecorder();

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {

        // 获取网络请求对象（实际请求数据）
        Request request = chain.request();

        // 存储实际连接数据
        HttpRealConnection realConnection = request.tag(HttpRealConnection.class);
        if (realConnection != null) {
            Connection connection = chain.connection();
            if (connection != null) {
                // 获取五元组信息
                Socket socket = connection.socket();

                // setLocalIp 操作比较耗时，占连接数据记录总时长的一半
                realConnection.setLocalIp(socket.getLocalAddress().getHostAddress());
                realConnection.setLocalPort(socket.getLocalPort());
                realConnection.setIp(socket.getInetAddress().getHostAddress());
                realConnection.setPort(socket.getPort());
                realConnection.setProtocol(connection.protocol().toString());

                // 获取加密信息
                Handshake handshake = connection.handshake();
                if (handshake != null) {
                    realConnection.setTlsVersion(handshake.tlsVersion().javaName());
                    realConnection.setCipher(handshake.cipherSuite().javaName());
                }

            }
        }

        return chain.proceed(request);
    }

}
