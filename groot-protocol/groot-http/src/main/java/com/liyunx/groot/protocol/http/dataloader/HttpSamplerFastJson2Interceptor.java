package com.liyunx.groot.protocol.http.dataloader;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.dataloader.fastjson2.AbstractFastJson2Interceptor;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.HttpSampler;
import com.liyunx.groot.protocol.http.config.HttpConfigItem;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.liyunx.groot.protocol.http.constants.MediaType;
import com.liyunx.groot.protocol.http.model.Part;
import com.liyunx.groot.testelement.TestElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HttpSamplerFastJson2Interceptor extends AbstractFastJson2Interceptor {

    @Override
    public <T extends ConfigItem<?>> Object deserializeConfigItem(Class<T> clazz, Object value) {
        if (HttpConfigItem.class.isAssignableFrom(clazz)) {
            if (value instanceof Map map) {
                // k 为 serviceName，v 为 HttpServiceConfigItem
                map.forEach((k, v) -> {
                    Map<String, Object> serviceConfig = (Map<String, Object>) v;
                    Object proxyValue = serviceConfig.get("proxy");
                    if (proxyValue instanceof String _proxyValue) {
                        HashMap<String, Object> proxyMap = new HashMap<>();
                        String[] ipAndPort = _proxyValue.split(":");
                        proxyMap.put("ip", ipAndPort[0].trim());
                        proxyMap.put("port", ipAndPort[1].trim());
                        serviceConfig.put("proxy", proxyMap);
                    }
                    managerMapToList(serviceConfig, "headers");
                });
                return value;
            }
        }
        return null;
    }

    @Override
    public <T extends TestElement<?>> Map<String, Object> deserializeTestElement(Class<T> clazz, Map<String, Object> value) {
        if (HttpSampler.class.equals(clazz)) {
            Object requestMap = value.get(HttpSampler.KEY);
            if (!(requestMap instanceof Map)) {
                return null;
            }
            Map request = (Map) requestMap;

            managerMapToList(request, "params");
            managerMapToList(request, "headers");
            managerMapToList(request, "form");
            multiPartMapOrList(request, "multipart");
            return value;
        }

        return null;
    }

    private void managerMapToList(Map request, String key) {
        if (request.get(key) instanceof Map value) {
            List<Map<String, String>> paramsList = managerMapToList(value);
            request.put(key, paramsList);
        }
    }

    private void multiPartMapOrList(Map request, String multipartKey) {
        Object multipart = request.get(multipartKey);
        if (multipart instanceof Map) {
            List<Map<String, Object>> multiPartList = multiPartMapToList((Map<String, Object>) multipart);
            request.put(multipartKey, multiPartList);
        } else if (multipart instanceof List) {
            List<Map<String, Object>> multiPartList = (List<Map<String, Object>>) multipart;
            multiPartListHeadersToList(multiPartList);
            request.put(multipartKey, multiPartList);
        }
    }

    private void multiPartListHeadersToList(List<Map<String, Object>> multiPartList) {
        for (Map<String, Object> part : multiPartList) {
            if (part.get("headers") instanceof Map headers) {
                List<Map<String, String>> headersList = managerMapToList(headers);
                part.put("headers", headersList);
            }
        }
    }

    // params/headers/form:
    //   k1: v1
    //   k2: v2
    //
    // params/headers/form:
    //   - name: k1
    //     value: v1
    //   - name: k2
    //     value: v2
    private List<Map<String, String>> managerMapToList(Map<String, ?> map) {
        List<Map<String, String>> list = new ArrayList<>();
        map.forEach((k, v) -> {
            Map<String, String> nameAndValue = new HashMap<>();
            nameAndValue.put("name", String.valueOf(k));
            nameAndValue.put("value", String.valueOf(v));
            list.add(nameAndValue);
        });
        return list;
    }

    // multipart:
    //   k1: textValue
    //   file:
    //     file: some.pdf
    //	   name: 一个文件.pdf
    //	   type: application/pdf
    //   k2:
    //     value: textValue
    //	   type: text/plain
    //   k3:
    //	   value:
    //	     n1: v1
    //		 n2:
    //		   u2: HiH
    //	   type: application/json
    private List<Map<String, Object>> multiPartMapToList(Map<String, Object> multipart) {
        // multipart 对应方法注释里 multipart 节点下的内容
        List<Map<String, Object>> list = new ArrayList<>();
        multipart.forEach((k, v) -> {
            // k,v 对应 k1: textValue
            Map<String, Object> part;
            if (v instanceof String) {
                part = partFromStringValue(k, (String) v);
            } else if (v instanceof Map) {
                part = partFromMapValue(k, (Map) v);
            } else {
                throw new InvalidDataException(
                    "不支持的类型，multipart.<name> 的值只能是 String / Map，当前值：%s", JSON.toJSONString(v));
            }
            list.add(part);
        });
        return list;
    }

    private Map<String, Object> partFromStringValue(String name, String body) {
        // <<简写>>
        // k1: textValue
        //
        // <<全写>>
        // 	- name: k1
        //	  headers:
        //	    - name: Content-Disposition
        //	      value: form-data; name="k1"
        //		- name: Content-Type
        //	      value: text/plain
        //	  body: textValue
        Map<String, Object> part = new HashMap<>();
        part.put("name", name);
        if ("file".equals(name)) {
            part.put("file", body);
        } else {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeader.CONTENT_DISPOSITION.value(), Part.dispositionHeaderValue(name, null));
            headers.put(HttpHeader.CONTENT_TYPE.value(), MediaType.TEXT_PLAIN.value());
            part.put("headers", managerMapToList(headers));
            part.put("body", body);
        }
        return part;
    }

    private Map<String, Object> partFromMapValue(String name, Map bodyAndHeaders) {
        // <<简写>>
        // file:
        //   file: some.pdf
        //   name: 一个文件.pdf
        //   type: application/pdf
        // k2:
        //   value: textValue
        //   type: text/plain
        // k3:
        //   value:
        //     n1: v1
        //     n2:
        //       u2: HiH
        //   type: application/json
        Map<String, Object> part = new HashMap<>();
        part.put("name", name);

        // 解析文件 Body
        Object file = bodyAndHeaders.get("file");
        if (file != null) {
            if (!(file instanceof String)) {
                throw new InvalidDataException(
                    "http.request.multipart.<name>.file 仅支持 String 类型的值，当前值：%s",
                    JSON.toJSONString(file));
            }

            Object filename = bodyAndHeaders.get("name");
            if (filename != null && !(filename instanceof String)) {
                throw new InvalidDataException(
                    "http.request.multipart.<name>.name 仅支持 String 类型的值，当前值：%s",
                    JSON.toJSONString(filename));
            }

            Object contentType = bodyAndHeaders.get("type");
            if (contentType != null && !(contentType instanceof String)) {
                throw new InvalidDataException(
                    "http.request.multipart.<name>.type 仅支持 String 类型的值，当前值：%s",
                    JSON.toJSONString(contentType));
            }

            Map<String, String> headers = new HashMap<>();
            if (filename != null) {
                headers.put(HttpHeader.CONTENT_DISPOSITION.value(), Part.dispositionHeaderValue(name, (String) filename));
            }
            if (contentType != null) {
                headers.put(HttpHeader.CONTENT_TYPE.value(), (String) contentType);
            }

            if (!headers.isEmpty()) {
                part.put("headers", managerMapToList(headers));
            }
            part.put("file", file);
            return part;
        }

        // 解析非文件 Body
        Object value = bodyAndHeaders.get("value");
        if (value != null) {
            Object contentType = bodyAndHeaders.get("type");
            if (contentType != null && !(contentType instanceof String)) {
                throw new InvalidDataException(
                    "http.request.multipart.<name>.type 仅支持 String 类型的值，当前值：%s",
                    JSON.toJSONString(contentType));
            }

            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeader.CONTENT_DISPOSITION.value(), Part.dispositionHeaderValue(name, null));
            if (contentType != null) {
                headers.put(HttpHeader.CONTENT_TYPE.value(), (String) contentType);
            }

            part.put("headers", managerMapToList(headers));
            part.put("body", value);
            return part;
        }

        throw new InvalidDataException("http.request.multipart.<name> 的值仅支持以下三种类型写法：\n" +
            "<name>: StringValue\n" +
            "<name>:\n" +
            "    file: StringValue\n" +
            "<name>:\n" +
            "    value: AnyValue");
    }

}
