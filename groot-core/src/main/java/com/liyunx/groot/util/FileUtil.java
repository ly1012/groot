package com.liyunx.groot.util;

import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.exception.InvalidPathException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件工具类
 */
public class FileUtil {

    /**
     * 获取指定目录下指定前缀的所有文件（不包含子目录）
     *
     * @param rootPath 指定目录
     * @param prefix   指定前缀
     * @return 符合条件的所有文件
     */
    public static List<File> getFilesStartWith(Path rootPath, String prefix) {
        // 合法性检查
        File rootFile = rootPath.toFile();
        if (!rootFile.exists()) {
            throw new InvalidPathException(String.format("指定路径 %s 不存在", rootPath.toAbsolutePath()));
        }
        if (!rootFile.isDirectory()) {
            throw new InvalidPathException(String.format("指定路径 %s 不是一个目录", rootPath.toAbsolutePath()));
        }

        // 查找并过滤
        try {
            return Files.list(rootPath)
                .filter(path -> {
                    File file = path.toFile();
                    if (file.isDirectory()) return false;
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith(prefix);
                })
                .map(path -> path.toFile())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new GrootException(e);
        }
    }

    /**
     * 读取文件内容，返回字符串内容
     *
     * @param path 文件路径
     * @return 文件内容
     */
    public static String readFile(Path path) {
        String text;
        try {
            text = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new GrootException(String.format("文件 %s 读取失败", path.toAbsolutePath()), e);
        }
        return text;
    }

    /**
     * 根据文件名创建新文件
     *
     *
     * <p>不同文件名对应的文件：
     * <pre>
     * b.txt       --&gt;  b.txt
     * a/b.txt     --&gt;  b.txt
     * a/b         --&gt;  b       注意这里 b 当做文件处理，而非文件夹
     * </pre>
     *
     * @param fileName 文件名，相对路径文件名或绝对路径文件名
     * @param delete   如果文件已存在是否删除，true 表示删除，false 表示保留原文件或文件夹
     */
    public static File createFileOrDirectory(String fileName, boolean isDirectory, boolean delete) {
        File file = new File(fileName);

        // 如果文件的父目录不存在，则创建
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            boolean isSuccessful = parentFile.mkdirs();
            if (!isSuccessful) {
                throw new GrootException("目录创建失败，%s", parentFile.getAbsolutePath());
            }
        }

        // 删除已存在文件或直接返回不做处理
        if (file.exists()) {
            if (!delete) {
                return file;
            }
            boolean isSuccessful = file.delete();
            if (!isSuccessful) {
                throw new GrootException("文件删除失败，%s", file.getAbsolutePath());
            }
        }

        // 创建新文件
        boolean isSuccessful = false;
        String type;
        try {
            if (isDirectory) {
                type = "文件夹";
                isSuccessful = file.mkdir();
            } else {
                type = "文件";
                isSuccessful = file.createNewFile();
            }
            if (!isSuccessful) {
                throw new GrootException("%s创建失败：%s", type, file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new GrootException("文件创建失败：%s", file.getAbsolutePath());
        }

        return file;
    }

    /**
     * 获取文件名的扩展名。
     *
     * <p>
     * This method returns the textual part of the fileName after the last dot.
     * There must be no directory separator after the dot.
     * </p>
     * <pre>
     * foo.txt      --&gt; "txt"
     * a/b/c.jpg    --&gt; "jpg"
     * a/b.txt/c    --&gt; ""
     * a/b/c        --&gt; ""
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is running on, with the
     * exception of a possible {@link IllegalArgumentException} on Windows (see below).
     * </p>
     * <p>
     * <b>Note:</b> This method used to have a hidden problem for names like "foo.exe:bar.txt".
     * In this case, the name wouldn't be the name of a file, but the identifier of an
     * alternate data stream (bar.txt) on the file foo.exe. The method used to return
     * ".txt" here, which would be misleading. Commons IO 2.7, and later versions, are throwing
     * an {@link IllegalArgumentException} for names like this.
     * </p>
     *
     * @param fileName the fileName to retrieve the extension of.
     * @return the extension of the file or an empty string if none exists or {@code null}
     * if the fileName is {@code null}.
     * @throws IllegalArgumentException <b>Windows only:</b> The fileName parameter is, in fact,
     *                                  the identifier of an Alternate Data Stream, for example "foo.exe:bar.txt".
     */
    public static String getExtension(final String fileName) throws IllegalArgumentException {
        return FilenameUtils.getExtension(fileName);
    }

}


