package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.common.Mergeable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.liyunx.groot.protocol.http.support.HttpModelSupport;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Http Request or Response Headers：单次请求的所有 Header 数据
 */
public class HeaderManager
    extends ArrayList<Header>
    implements Copyable<HeaderManager>, Mergeable<HeaderManager>, Computable<HeaderManager> {

    public static class HeaderManagerObjectReader implements ObjectReader<HeaderManager> {

        @SuppressWarnings("rawtypes")
        @Override
        public HeaderManager readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            List headersList = jsonReader.readArray();
            HeaderManager headers = new HeaderManager();
            for (Object headerMap : headersList) {
                if (!(headerMap instanceof Map)) {
                    throw new InvalidDataException("http.headers/http.multipart[*].headers 数据结构非法，当前值：%s", JSON.toJSONString(headerMap));
                }
                headers.add(JSON.parseObject(JSON.toJSONString(headerMap), Header.class));
            }
            return headers;
        }
    }

    public static HeaderManager of(Map<String, String> map) {
        HeaderManager headerManager = new HeaderManager();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            headerManager.add(new Header(k, v));
        }
        return headerManager;
    }

    public static HeaderManager of(Header header) {
        HeaderManager headerManager = new HeaderManager();
        headerManager.add(header);
        return headerManager;
    }

    public void removeHeaders(String headerName) {
        removeIf(header -> header.name.equalsIgnoreCase(headerName));
    }

    public void setHeader(String headerName, String headerValue) {
        removeHeaders(headerName);
        add(new Header(headerName, headerValue));
    }

    public Header getHeader(String headerName) {
        List<Header> list = getHeaders(headerName);
        if (list.isEmpty())
            return null;
        return list.get(0);
    }

    public Header getHeader(HttpHeader headerName) {
        return getHeader(headerName.value());
    }

    public String get(String headerName) {
        Header header = getHeader(headerName);
        if (header == null) return null;
        return header.getValue();
    }

    public List<Header> getHeaders(String headerName) {
        List<Header> list = new ArrayList<>();
        for (Header header : this) {
            if (header.name.equalsIgnoreCase(headerName)) {
                list.add(header);
            }
        }
        return list;
    }

    public Map<String, String> getCookies() {
        Header cookieHeader = getHeader(HttpHeader.COOKIE.value());
        if (cookieHeader == null)
            return null;

        Map<String, String> cookies = new HashMap<>();
        String cookieValue = cookieHeader.value;
        String[] nameValues = cookieValue.split(";");
        for (String nameValue : nameValues) {
            String[] nameValueArray = nameValue.trim().split("=");
            String name = nameValueArray[0];
            String value;
            if (nameValueArray.length == 2) {
                value = nameValueArray[1];
            } else {
                value = "";
            }
            cookies.put(name, value);
        }
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty())
            return;

        // Headers 中存在的 Cookie Header 和单独声明的 cookies 合并
        Map<String, String> mergedCookies = cookies;
        Map<String, String> headerCookies = getCookies();
        if (headerCookies != null) {
            headerCookies.putAll(cookies);
            mergedCookies = headerCookies;
        }

        // 构建 Cookie 值
        StringBuilder cookieValueBuilder = new StringBuilder();
        int size = mergedCookies.size();
        int cnt = 1;
        for (Map.Entry<String, String> entry : mergedCookies.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            cookieValueBuilder
                .append(name)
                .append("=")
                .append(value);
            if (cnt != size)
                cookieValueBuilder.append("; ");
            cnt++;
        }

        setHeader(HttpHeader.COOKIE.value(), cookieValueBuilder.toString());
    }

    @Override
    public HeaderManager copy() {
        HeaderManager res = new HeaderManager();
        forEach(p -> res.add(p.copy()));
        return res;
    }

    @Override
    public HeaderManager merge(HeaderManager other) {
        return HttpModelSupport.multiValueManagerMerge(this, other, e -> e.name);
    }

    @Override
    public HeaderManager eval(ContextWrapper ctx) {
        forEach(ctx::eval);
        return this;
    }

}
