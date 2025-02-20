package com.liyunx.groot.dataloader.file;

import java.util.stream.Stream;

/**
 * 文件类型常量
 */
public enum  FileType {

    YML,
    YAML,
    JSON,
    XML,
    CSV,
    XLS,
    XLSX;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public boolean equalsIgnoreCase(String fileType){
        return !isNullOrEmpty(fileType)
            && this.name().equalsIgnoreCase(fileType.trim());
    }

    public static boolean isJSONFile(String fileType){
        return !isNullOrEmpty(fileType)
            && JSON.equalsIgnoreCase(fileType.trim());
    }

    public static boolean isYamlFile(String fileType){
        return !isNullOrEmpty(fileType)
            && (YML.equalsIgnoreCase(fileType.trim()) || YAML.equalsIgnoreCase(fileType.trim()));
    }

    public static boolean isJSONOrYamlFile(String fileType){
        return !isNullOrEmpty(fileType)
            && (JSON.equalsIgnoreCase(fileType.trim())
            || YML.equalsIgnoreCase(fileType.trim())
            || YAML.equalsIgnoreCase(fileType.trim()));
    }

    public static boolean isXMLFile(String fileType){
        return !isNullOrEmpty(fileType) && XML.equalsIgnoreCase(fileType.trim());
    }

    public static boolean isTextFile(String fileType){
        return !isNullOrEmpty(fileType)
            && (Stream.of(JSON, YML, YAML, XML).anyMatch(type -> type.equalsIgnoreCase(fileType.trim())));
    }

    public static boolean isCSVFile(String fileType){
        return !isNullOrEmpty(fileType) && CSV.equalsIgnoreCase(fileType.trim());
    }

    public static boolean isExcelFile(String fileType){
        return !isNullOrEmpty(fileType)
            && (Stream.of(XLS, XLSX).anyMatch(type -> type.equalsIgnoreCase(fileType.trim())));
    }

    private static boolean isNullOrEmpty(String fileType){
        return fileType == null || fileType.trim().isEmpty();
    }

}
