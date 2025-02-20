package com.liyunx.groot.protocol.http.constants;

import com.liyunx.groot.util.FileUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * MediaType 常量
 */
public enum MediaType {

    APPLICATION_JSON("application/json"),
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART_FORM_DATA("multipart/form-data"),

    APPLICATION_OCTET_STREAM("application/octet-stream"),

    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_GIF("image/gif"),

    AUDIO_MP3("audio/mp3"),
    AUDIO_WAV("audio/wav"),

    VIDEO_AVI("video/avi"),
    VIDEO_MPEG4("video/mpeg4"),

    TEXT_HTML("text/html"),
    APPLICATION_XML("application/xml"),
    TEXT_PLAIN("text/plain"),

    APPLICATION_VND_MS_EXCEL("application/vnd.ms-excel"),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET
        ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    APPLICATION_PDF("application/pdf"),
    APPLICATION_MSWORD("application/msword"),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT
        ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    APPLICATION_VND_MS_POWERPOINT("application/vnd.ms-powerpoint"),
    APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_PRESENTATION
        ("application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    APPLICATION_X_MSDOWNLOAD("application/x-msdownload");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    private static final Map<String, String> EXTENSION_NAME_MEDIA_TYPE_MAP = new HashMap<String, String>() {{

        // 图像文件
        put("png", IMAGE_PNG.value);
        put("jpg", IMAGE_JPEG.value);
        put("jpeg", IMAGE_JPEG.value);
        put("gif", IMAGE_GIF.value);

        // 音频文件
        put("mp3", AUDIO_MP3.value);
        put("wav", AUDIO_WAV.value);

        // 视频文件
        put("avi", VIDEO_AVI.value);
        put("mp4", VIDEO_MPEG4.value);

        // 文本文件
        put("htm", TEXT_HTML.value);
        put("html", TEXT_HTML.value);
        put("txt", TEXT_PLAIN.value);
        put("jmx", APPLICATION_XML.value);

        // Office 文件
        put("xls", APPLICATION_VND_MS_EXCEL.value);
        put("xlsx", APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET.value);
        put("pdf", APPLICATION_PDF.value);
        put("doc", APPLICATION_MSWORD.value);
        put("docx", APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT.value);
        put("ppt", APPLICATION_VND_MS_POWERPOINT.value);
        put("pptx", APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_PRESENTATION.value);

        put("exe", APPLICATION_X_MSDOWNLOAD.value);

    }};

    public static String getMediaTypeByExtensionName(String extensionName) {
        String mediaType = EXTENSION_NAME_MEDIA_TYPE_MAP.get(extensionName);
        if (mediaType == null) {
            mediaType = APPLICATION_OCTET_STREAM.value;
        }
        return mediaType;
    }

    public static String getMediaTypeByFileName(String fileName) {
        String extName = FileUtil.getExtension(fileName);
        return getMediaTypeByExtensionName(extName);
    }

}
