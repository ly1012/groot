package com.liyunx.groot.dataloader.file;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.config.EnvironmentConfig;
import com.liyunx.groot.dataloader.EnvironmentConfigLoader;
import com.liyunx.groot.exception.InitializationException;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.util.YamlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

/**
 * 从本地文件中加载环境数据
 */
public class EnvironmentConfigFileLoader implements EnvironmentConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentConfigFileLoader.class);

    // 前缀可以是 env/ 表示 env 目录，比如 env/test.yaml
    private String environmentFilePrefix = "env-";
    private String environmentFileName;

    @Override
    public EnvironmentConfig load(String environmentName) {
        environmentFileName = environmentFilePrefix + environmentName;

        // 定义支持的文件扩展名及其对应的 FileType
        String[] extensions = {"yml", "yaml", "json"};
        FileType[] fileTypes = {FileType.YML, FileType.YAML, FileType.JSON};

        // 循环查找第一个符合条件的全局配置文件：yml > yaml > json
        File envFile = null;
        FileType fileType = null;
        for (int i = 0; i < extensions.length; i++) {
            envFile = getFileByExtension(extensions[i]);
            if (envFile != null) {
                fileType = fileTypes[i];
                break;
            }
        }

        if (envFile == null) {
            throw new IllegalArgumentException(String.format("环境配置文件 %s/%s.{yml,yaml,json} 未找到",
                ApplicationConfig.getWorkDirectory(), environmentFileName));
        }

        return toEnvironment(envFile, fileType);
    }

    private File getFileByExtension(String extensionName) {
        File file = Paths.get(ApplicationConfig.getWorkDirectory(),
            environmentFileName + "." + extensionName).toFile();
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    private EnvironmentConfig toEnvironment(File file, FileType fileType) {
        // 合法性检查：非空检查
        String text = FileUtil.readFile(file.toPath());
        if (text == null || text.trim().isEmpty()) {
            LOGGER.info("文件 {} 内容为空，使用空配置", file.getAbsolutePath());
            return null;
        }

        // Yaml 文件加载
        if (FileType.isYamlFile(fileType.name())) {
            JSONObject jsonObject = YamlUtil.getYaml().loadAs(text, JSONObject.class);
            return jsonStringToEnvironment(file, JSON.toJSONString(jsonObject));
        }

        // JSON 文件加载
        if (FileType.isJSONFile(fileType.name())) {
            return jsonStringToEnvironment(file, text);
        }

        throw new InitializationException(String.format("环境配置文件 %s 加载失败，当前仅支持 JSON 或 Yaml 文件", file.getName()));
    }

    private EnvironmentConfig jsonStringToEnvironment(File file, String json) {
        try {
            return JSON.parseObject(json, EnvironmentConfig.class);
        } catch (JSONException e){
            throw new InvalidDataException("环境配置加载失败：%s 文件数据不合法", e, file.getAbsolutePath());
        }
    }

    public void setEnvironmentFilePrefix(String environmentFilePrefix) {
        this.environmentFilePrefix = environmentFilePrefix;
    }

}
