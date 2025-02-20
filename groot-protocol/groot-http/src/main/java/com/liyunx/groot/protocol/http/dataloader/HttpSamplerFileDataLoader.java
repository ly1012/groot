package com.liyunx.groot.protocol.http.dataloader;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.liyunx.groot.dataloader.file.FileType;
import com.liyunx.groot.dataloader.file.LocalDataLoader;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.HttpAPI;
import com.liyunx.groot.protocol.http.HttpSampler;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.util.YamlUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HttpAPI 与 HttpSampler 加载类
 */
@SuppressWarnings("rawtypes")
public class HttpSamplerFileDataLoader extends LocalDataLoader {

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T next(String text, String textType, Class<T> clazz) {
        if (HttpAPI.class.equals(clazz)) {
            requireNotEmpty(text, textType, clazz);
            if (FileType.isJSONFile(textType)) {
                return (T) JSON.parseObject(text, HttpAPI.class);
            }
            if (FileType.isYamlFile(textType)) {
                JSONObject jsonObject = YamlUtil.getYaml().loadAs(text, JSONObject.class);
                return (T) JSON.parseObject(JSON.toJSONString(jsonObject), HttpAPI.class);
            }
            return null;
        }
        if (HttpSampler.class.equals(clazz)) {
            requireNotEmpty(text, textType, clazz);
            if (FileType.isJSONFile(textType)) {
                return (T) JSON.parseObject(text, TestElement.class);
            }
            if (FileType.isYamlFile(textType)) {
                JSONObject jsonObject = YamlUtil.getYaml().loadAs(text, JSONObject.class);
                String jsonStr = JSON.toJSONString(jsonObject);
                return (T) JSON.parseObject(jsonStr, TestElement.class);
            }
        }
        return null;
    }

    @Override
    protected <T> T nextByID(String identifier, Class<T> clazz) {
        boolean isAPI = HttpAPI.class.equals(clazz);
        boolean isSampler = HttpSampler.class.equals(clazz);
        if (!(isAPI || isSampler)) {
            return null;
        }

        Path path = getAbsolutePath(identifier);
        String text = FileUtil.readFile(path);
        String textType = getFileType(path);

        if (text == null || text.trim().isEmpty()) {
            throw new InvalidDataException("文件 %s 内容为空", path.toAbsolutePath().toString());
        }

        // 从路径中提取 serviceName

        // 统一转为 JSON
        LinkedHashMap jsonObject;
        if (FileType.isYamlFile(textType)) {
            jsonObject = YamlUtil.getYaml().loadAs(text, LinkedHashMap.class);
            textType = FileType.JSON.toString();
        } else if (FileType.isJSONFile(textType)) {
            jsonObject = JSON.parseObject(text);
        } else {
            return null;
        }

        // 判断是否已存在 serviceName
        boolean hasServiceName = hasServiceName(jsonObject, isAPI);

        // 如果不存在 serviceName，尝试从路径中提取
        if (!hasServiceName && !Paths.get(identifier).isAbsolute()) {
            String serviceName = getServiceNameFromPath(identifier);
            if (serviceName != null) {
                if (isAPI) {
                    JSONPath.set(jsonObject, "$.service", serviceName);
                } else {
                    JSONPath.set(jsonObject, "$.http.service", serviceName);
                }
            }
        }

        return next(JSON.toJSONString(jsonObject), textType, clazz);
    }

    private boolean hasServiceName(Map jsonObject, boolean isAPI) {
        if (isAPI) {
            return jsonObject.get("service") != null;
        } else {
            return jsonObject.get("http") != null &&
                ((Map) jsonObject.get("http")).get("service") != null;
        }
    }

    // apis/UserService/login/login.yml
    private String getServiceNameFromPath(String path) {
        String name = null;
        try {
            name = path.split(Pattern.quote("/"))[1];
        } catch (Exception e) {
            // do nothing.
        }
        return name;
    }

}
