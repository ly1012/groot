package com.liyunx.groot.dataloader.file;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.config.GlobalConfig;
import com.liyunx.groot.dataloader.GlobalConfigLoader;
import com.liyunx.groot.exception.InitializationException;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.util.YamlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

/**
 * 从本地文件中加载全局数据
 */
public class GlobalConfigFileLoader implements GlobalConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalConfigFileLoader.class);

    private String globalFileName = "global";

    @Override
    public GlobalConfig load() {
        // 定义支持的文件扩展名及其对应的 FileType
        String[] extensions = {"yml", "yaml", "json"};

        // 循环查找第一个符合条件的全局配置文件：yml > yaml > json
        File globalFile = null;
        String fileType = null;
        for (String extension : extensions) {
            globalFile = getFileByExtension(extension);
            if (globalFile != null) {
                fileType = extension;
                break;
            }
        }

        if (globalFile == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("全局配置文件未找到");
            }
            return null;
        }

        return toGlobalConfig(globalFile, fileType);
    }

    private File getFileByExtension(String extensionName) {
        File file = Paths.get(ApplicationConfig.getWorkDirectory(), globalFileName + "." + extensionName).toFile();
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    private GlobalConfig toGlobalConfig(File file, String fileType) {
        // 合法性检查：非空检查
        String text = FileUtil.readFile(file.toPath());
        if (text == null || text.trim().isEmpty()) {
            LOGGER.info("文件 {} 内容为空，使用空配置", file.getAbsolutePath());
            return null;
        }

        // Yaml 文件加载
        if (FileType.isYamlFile(fileType)) {
            JSONObject jsonObject = YamlUtil.getYaml().loadAs(text, JSONObject.class);
            return jsonStringToGlobalConfig(file, JSON.toJSONString(jsonObject));
        }

        // JSON 文件加载
        if (FileType.isJSONFile(fileType)) {
            return jsonStringToGlobalConfig(file, text);
        }

        throw new InitializationException(String.format("全局配置文件 %s 加载失败，当前仅支持 JSON 或 Yaml 文件", file.getName()));
    }

    private GlobalConfig jsonStringToGlobalConfig(File file, String json) {
        try {
            return JSON.parseObject(json, GlobalConfig.class);
        } catch (JSONException e) {
            throw new InvalidDataException("全局配置加载失败：%s 文件数据不合法", e, file.getAbsolutePath());
        }
    }

    public void setGlobalFileName(String globalFileName) {
        this.globalFileName = globalFileName;
    }

}
