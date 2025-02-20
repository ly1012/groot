package com.liyunx.groot.support;

import com.liyunx.groot.exception.GrootException;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.liyunx.groot.constants.GrootConstants.GROOT_BASE_PACKAGE_NAME;
import static com.liyunx.groot.support.ExtensibleResourceGenerator.getCurrentProjectPath;

/**
 * ExtensibleXXXBuilder 支持类
 */
public class ExtensibleSourceGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExtensibleSourceGenerator.class);

    private static final Map<String, String> classHeaders = new HashMap<>();

    static {
        classHeaders.put("ExtensibleAllConfigBuilder", """
            /**
             * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br/>
             *
             * 所有配置构建，适用于 TestCase 或各种 Controller 中间层
             */
            public class ExtensibleAllConfigBuilder extends ExtensibleCommonConfigBuilder<ExtensibleAllConfigBuilder> {
                        
            """);
        classHeaders.put("ExtensibleCommonConfigBuilder", """
            /**
             * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br/>
             * 额外的公共配置构建
             *
             * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共配置项（项目中 groot-core 以外的 Jar）。
             */
            public abstract class ExtensibleCommonConfigBuilder<T extends ExtensibleCommonConfigBuilder<T>>
                extends AbstractTestElement.ConfigBuilder<T> {
                        
            """);
        classHeaders.put("ExtensibleCommonPreProcessorsBuilder", """
            /**
             * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br/>
             * 额外的公共前置处理器构建
             *
             * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共前置处理器。
             */
            public abstract class ExtensibleCommonPreProcessorsBuilder<SELF extends ExtensibleCommonPreProcessorsBuilder<SELF>>
                extends AbstractTestElement.PreProcessorsBuilder<SELF> {
                        
            """);
        classHeaders.put("ExtensibleCommonExtractorsBuilder", """
            /**
             * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br/>
             * 额外的公共提取器构建
             *
             * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共提取器。
             */
            public abstract class ExtensibleCommonExtractorsBuilder<T extends ExtensibleCommonExtractorsBuilder<T>>
                extends AbstractTestElement.ExtractorsBuilder<T> {
                        
            """);
        classHeaders.put("ExtensibleCommonAssertionsBuilder", """
            /**
             * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br/>
             * 额外的公共断言构建
             *
             * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共断言。
             */
            public abstract class ExtensibleCommonAssertionsBuilder<T extends ExtensibleCommonAssertionsBuilder<T>>
                extends AbstractTestElement.AssertionsBuilder<T> {
                        
            """);
        classHeaders.put("ExtensibleCommonPostProcessorsBuilder", """
            /**
             * <b>由 ExtensibleSourceGenerator 自动生成，禁止直接修改</b><br/>
             * 额外的公共后置处理器构建
             *
             * <p>本类为覆盖层，可以通过类覆盖来添加额外的公共后置处理器。
             */
            public abstract class ExtensibleCommonPostProcessorsBuilder<
                SELF extends ExtensibleCommonPostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER>,
                EXTRACT_BUILDER extends AbstractTestElement.ExtractorsBuilder<EXTRACT_BUILDER>,
                ASSERT_BUILDER extends AbstractTestElement.AssertionsBuilder<ASSERT_BUILDER>>
                extends AbstractTestElement.PostProcessorsBuilder<SELF, EXTRACT_BUILDER, ASSERT_BUILDER> {

                public ExtensibleCommonPostProcessorsBuilder(AbstractTestElement.Builder<?, ?, ?, ?, ?, ?, ?> elementBuilder) {
                    super(elementBuilder);
                }

            """);
    }

    /**
     * 读取依赖组件的扩展类资源文件，并生成对应类的源码文件
     */
    public static void generateSources(Path projectPath) {
        // 计算源码文件所在路径
        Path sourceFileRootPath = resolvesourceFileRootPath(projectPath);

        // 读取资源文件生成对应源码，并写入文件
        for (String className : classHeaders.keySet()) {
            String source = generateSource(className);
            if (!source.isEmpty()) {
                File sourceFile = sourceFileRootPath.resolve(className + ".java").toFile();
                try {
                    FileUtils.writeStringToFile(sourceFile, source, "UTF-8");
                    log.info("生成源码文件: {}", sourceFile.getAbsolutePath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static Path resolvesourceFileRootPath(Path projectPath) {
        Path sourceFileRootPath = Paths.get(projectPath
            .resolve("src/main/java/" + GROOT_BASE_PACKAGE_NAME.replace('.', '/') + "/builder")
            .normalize()
            .toString()
            .replace("file:", ""));
        log.info("源码文件根路径: {}", sourceFileRootPath);
        return sourceFileRootPath;
    }

    // 根据资源 ID 生成对应扩展类的源码并格式化
    private static String generateSource(String className) {
        // 读取该扩展类的所有资源文件路径
        ClassLoader classLoader = ExtensibleSourceGenerator.class.getClassLoader();
        Enumeration<URL> urls = null;
        try {
            urls = classLoader.getResources("groot/builder/" + className);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 读取所有资源文件
        List<String> sources = new ArrayList<>();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (log.isDebugEnabled()) {
                log.debug("read resource: {}", url);
            }
            try (InputStream inputStream = url.openStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {

                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                sources.add(content.toString().trim());
            } catch (IOException e) {
                throw new GrootException(e);
            }
        }
        if (sources.isEmpty()) {
            return "";
        }

        // 合并多个资源文件内容，生成 Java 源码
        Set<String> imports = new HashSet<>();
        StringBuilder classBody = new StringBuilder();
        for (String source : sources) {
            try (StringReader stringReader = new StringReader(source);
                 BufferedReader reader = new BufferedReader(stringReader)) {
                String line;
                boolean isImport = true;
                while ((line = reader.readLine()) != null) {
                    if (isImport) {
                        if (line.trim().isEmpty()) {
                            isImport = false;
                            continue;
                        }
                        imports.add(line);
                    } else {
                        classBody.append(line).append("\n");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String importCode = imports.stream().sorted().collect(Collectors.joining("\n"));
        StringBuilder sourceCode = new StringBuilder();
        sourceCode
            .append("package ").append(GROOT_BASE_PACKAGE_NAME).append(".builder;\n")
            .append("\n")
            .append(importCode)
            .append("\n\n")
            .append(classHeaders.get(className))
            .append("\n")
            .append(classBody)
            .append("}");

        try {
            JavaFormatterOptions options = JavaFormatterOptions.builder()
                .style(JavaFormatterOptions.Style.GOOGLE)
                .formatJavadoc(true)
                .reorderModifiers(true)
                .build();
            return new Formatter(options)
                .formatSourceAndFixImports(sourceCode.toString());
        } catch (IllegalAccessError e) {
            // https://github.com/google/google-java-format
            // The following JVM flags are required when running on JDK 16 and newer,
            // due to JEP 396: Strongly Encapsulate JDK Internals by Default
            // --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
            // --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
            // --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
            // --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
            // --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
            // --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
            log.warn("Failed to format source code by google-java-format, use unformatted source code.");
            return sourceCode.toString();
        } catch (FormatterException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        if (args.length > 1) {
            generateSources(Paths.get(args[0]));
        } else {
            generateSources(getCurrentProjectPath());
        }
    }

}
