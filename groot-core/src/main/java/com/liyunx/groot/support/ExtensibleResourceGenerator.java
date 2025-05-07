package com.liyunx.groot.support;

import com.liyunx.groot.builder.*;
import com.liyunx.groot.constants.GrootConstants;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.util.FileUtil;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * ExtensibleXXXBuilder 资源生成
 */
public class ExtensibleResourceGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExtensibleResourceGenerator.class);

    private static final Set<String> EXTENSIBLE_CLASS_NAMES = Set.of(
        ExtensibleAllConfigBuilder.class.getSimpleName(),
        ExtensibleCommonConfigBuilder.class.getSimpleName(),
        ExtensibleCommonPreProcessorsBuilder.class.getSimpleName(),
        ExtensibleCommonExtractorsBuilder.class.getSimpleName(),
        ExtensibleCommonAssertionsBuilder.class.getSimpleName(),
        ExtensibleCommonPostProcessorsBuilder.class.getSimpleName()
    );

    static Path getCurrentProjectPath() {
        ClassLoader classLoader = ExtensibleSourceGenerator.class.getClassLoader();
        URL outputDirectoryUrl = classLoader.getResource("");
        if (nonNull(outputDirectoryUrl) && !outputDirectoryUrl.toString().startsWith("jar:file:/")) {
            Path projectPath = outputDirectory2ProjectPath(outputDirectoryUrl.toString());
            log.info("使用 ClassLoader 获取当前项目路径：{}", projectPath);
            return projectPath;
        }

        // 如果第一次编译，因为 generate-sources 在 compile 之前，此时还未生成 target/classes 目录
        // 会返回 jar:file:/Users/yun/.m2/repository/org/slf4j/slf4j-api/2.0.4/slf4j-api-2.0.4.jar!/META-INF/versions/9/

        // 如果是 exec-maven-plugin exec:java，此时无法准确从 java.class.path 或 user.dir 等属性中获取当前项目路径，
        // 但根据源码，调用 Main 类前会将 classpath 加入 URLClassLoader
        {
            String outputDirectory = findOutputDirectoryFromUrlClassLoader(classLoader);
            if (nonNull(outputDirectory)) {
                Path projectPath = outputDirectory2ProjectPath(outputDirectory);
                log.info("使用 URLClassLoader 获取当前项目路径：{}", projectPath);
                return projectPath;
            }
        }

        // 如果是 exec-maven-plugin exec:exec
        {
            String outputDirectory = findOutputDirectoryFromClassPath();
            if (nonNull(outputDirectory)) {
                Path projectPath = outputDirectory2ProjectPath(outputDirectory);
                log.info("使用 java.class.path 获取当前项目路径：{}", projectPath);
                return projectPath;
            }
        }

        throw new GrootException("无法获取当前项目路径");
    }

    private static Path outputDirectory2ProjectPath(String outputDirectory) {
        return Paths.get(outputDirectory).resolve("../../").normalize();
    }

    private static String findOutputDirectoryFromClassPath() {
        String classpath = System.getProperty("java.class.path");
        if (isNull(classpath) || classpath.isEmpty()) {
            return null;
        }

        String[] classpathArray = classpath.split(File.pathSeparator);
        for (String path : classpathArray) {
            if (path.endsWith("target/classes")) {
                return path;
            }
        }

        return null;
    }

    private static String findOutputDirectoryFromUrlClassLoader(ClassLoader classLoader) {
        if (!(classLoader instanceof URLClassLoader urlClassLoader)) {
            return null;
        }
        for (URL url : urlClassLoader.getURLs()) {
            String urlPath = url.getPath();
            if (urlPath.endsWith("target/classes")) {
                return urlPath;
            }
        }
        return null;
    }

    /**
     * 将 src/test/java 目录的 com.liyunx.groot.builder 包下的扩展类，打包到 src/main/resources/groot/builder
     */
    public static void generateResources(Path projectPath) {
        // 获取扩展类所在文件夹路径
        Path sourceFileRootPath = resolveSourceFileRootPath(projectPath);
        // 获取资源生成目标路径
        Path resourceFileRootPath = resolveResourceFileRootPath(projectPath);

        // 读取扩展类源码生成资源文件到当前项目的 src/main/resources/groot/builder 目录
        File sourceFileRootDirectory = sourceFileRootPath.toFile();
        if (!(sourceFileRootDirectory.exists() && sourceFileRootDirectory.isDirectory())) {
            log.info("{} 目录不存在，跳过资源生成", sourceFileRootDirectory.getAbsolutePath());
            return;
        }

        File[] files = sourceFileRootDirectory.listFiles();
        if (files == null) {
            log.info("{} 目录为空，跳过资源生成", sourceFileRootDirectory.getAbsolutePath());
            return;
        }
        for (File file : files) {
            // 处理单个扩展类
            if (isExtensibleClass(file.getName())) {
                log.info("准备生成扩展类 {} 的资源文件", file.getAbsolutePath());
                generateResource(resourceFileRootPath, file);
            }
        }
    }

    private static Path resolveSourceFileRootPath(Path projectPath) {
        Path sourceFileRootPath = Paths.get(projectPath
            .resolve(
                "src/test/java/"
                    + GrootConstants.GROOT_BASE_PACKAGE_NAME.replace('.', '/')
                    + "/builder")
            .normalize()
            .toString()
            .replace("file:", ""));
        log.info("扩展类所在文件夹路径：{}", sourceFileRootPath);
        return sourceFileRootPath;
    }

    private static Path resolveResourceFileRootPath(Path projectPath) {
        Path resourceFileRootPath = Paths.get(projectPath
            .resolve("src/main/resources/groot/builder")
            .normalize()
            .toString()
            .replace("file:", ""));
        log.info("资源生成目标路径：{}", resourceFileRootPath);
        return resourceFileRootPath;
    }

    private static boolean isExtensibleClass(String fileName) {
        if (!fileName.endsWith(".java")) {
            return false;
        }
        String className = fileName.substring(0, fileName.length() - 5);
        return EXTENSIBLE_CLASS_NAMES.contains(className);
    }

    private static void generateResource(Path resourceFileRootPath, File sourceFile) {
        String fileName = sourceFile.getName();
        String className = fileName.substring(0, fileName.lastIndexOf("."));

        // 源码解析
        JavaProjectBuilder builder = new JavaProjectBuilder();
        JavaSource src;
        try (FileReader reader = new FileReader(sourceFile)) {
            src = builder.addSource(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 获取所有 import
        List<String> imports = src.getImports();

        // 获取类的内容
        List<String> code = new ArrayList<>();
        List<JavaClass> classes = src.getClasses();
        if (classes.size() != 1) {
            throw new InvalidDataException("%s.java 中不止一个类，不符合 ExtensibleXXXBuilder 规范", className);
        }
        JavaClass extensibleClass = classes.get(0);
        extensibleClass.getMethods().forEach(method -> {
            // TODO 处理限定类名问题
            //  初步想法：遍历 import 进行文本替换，不过有风险，可能存在类似的字符串
            //  或者自己组装方法源码，这样只需要处理方法签名，因为方法体没有限定类名的问题
            code.add(method.getCodeBlock());
        });

        if (code.isEmpty()) {
            return;
        }

        // 组装资源内容
        // 资源文件内容包括两部分：import 和 代码，import 后面是空行，用作两部分的分隔
        StringBuilder content = new StringBuilder();
        for (String importStr : imports) {
            content.append("import ").append(importStr).append(";\n");
        }
        content.append("\n");
        for (String method : code) {
            content.append(method).append("\n");
        }

        // 写入当前项目的资源文件
        File resourceFile = resourceFileRootPath.resolve(className).toFile();
        FileUtil.createFileOrDirectory(resourceFile.getAbsolutePath(), false, true);
        try {
            FileUtils.writeStringToFile(resourceFile, content.toString(), StandardCharsets.UTF_8);
            log.info("生成资源文件：{}", resourceFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            generateResources(Paths.get(args[0]));
        } else {
            generateResources(getCurrentProjectPath());
        }
    }


}
