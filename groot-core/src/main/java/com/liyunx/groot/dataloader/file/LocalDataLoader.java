package com.liyunx.groot.dataloader.file;

import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.dataloader.AbstractDataLoader;
import com.liyunx.groot.exception.InvalidPathException;
import com.liyunx.groot.util.StringUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 本地文件加载（框架默认的数据加载器）
 *
 * <p>TestElement 扩展时，至少提供本地文件加载实现，并继承该抽象类，注册 SPI。
 */
public abstract class LocalDataLoader extends AbstractDataLoader {

    protected static final String workDirectory = ApplicationConfig.getWorkDirectory();
    protected static final FileType DEFAULT_FILE_TYPE = FileType.YML;

    /**
     * identifier 合法性校验与路径解析
     *
     * @param identifier      文件路径（绝对路径或相对路径）。相对路径的工作目录为 ApplicationConfig.workDirectory
     * @param defaultFileType 默认文件后缀名，当缺失后缀名时自动补全，如果为 null 则不补全
     * @return 可访问的绝对路径
     */
    protected static Path getAbsolutePath(String identifier, String defaultFileType) {
        Path path = Paths.get(identifier);
        // 路径检查：如果是相对路径，自动拼接得到可访问路径
        if (!path.isAbsolute()) {
            path = Paths.get(workDirectory).resolve(path).normalize();
        }
        // 文件扩展名检查：若缺失扩展名，使用默认扩展名
        if (defaultFileType != null) {
            String fileName = path.getFileName().toString();
            if (!fileName.contains(".")) {
                path = Paths.get(path + "." + DEFAULT_FILE_TYPE);
            }
        }
        // 文件检查：文件不存在，则抛出异常
        if (!path.toFile().exists()) {
            throw new InvalidPathException(String.format("指定路径 %s 不存在", path.toAbsolutePath()));
        }
        return path;
    }

    /**
     * 默认文件后缀名为 {@link #DEFAULT_FILE_TYPE}
     *
     * @see #getAbsolutePath(String, String)
     */
    protected static Path getAbsolutePath(String identifier) {
        return getAbsolutePath(identifier, DEFAULT_FILE_TYPE.toString());
    }

    /**
     * 获取文件类型（扩展名）
     *
     * @param path 文件路径
     * @return 文件扩展名，全小写
     */
    protected static String getFileType(Path path) {
        return StringUtil.splitAndGetLastString(path.toString(), Pattern.quote(".")).toLowerCase();
    }

}
