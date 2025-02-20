package com.liyunx.groot.template.freemarker;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.liyunx.groot.ApplicationConfig;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.template.TemplateEngine;
import freemarker.template.*;
import freemarker.template.utility.DeepUnwrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

import static com.liyunx.groot.constants.ExpressionVariable.*;

/**
 * FreeMarker 模板引擎
 */
public class FreeMarkerTemplateEngine implements TemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(FreeMarkerTemplateEngine.class);

    private static final LogHelper logHelper = new LogHelper();

    private static class SingletonHolder {

        private static final Configuration cfg;

        static {
            cfg = new Configuration(Configuration.VERSION_2_3_32);
            // 模板文件编码
            cfg.setDefaultEncoding("UTF-8");
            // 模板运行异常处理器
            cfg.setTemplateExceptionHandler(new GrootTemplateExceptionHandler());
            cfg.setLogTemplateExceptions(false);
            // 将模板处理过程中抛出的 unchecked 异常包装为 TemplateException
            cfg.setWrapUncheckedExceptions(true);
            // Do not fall back to higher scopes when reading a null loop variable:
            cfg.setFallbackOnNullLoopVariable(false);
            // To accomodate to how JDBC returns values; see Javadoc!
            cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault());
            cfg.setShowErrorTips(false);
            // 设置自定义对象包装器
            cfg.setObjectWrapper(new GrootObjectWrapper(cfg.getIncompatibleImprovements()));
            // 设置布尔变量 convert boolean to string automatically，效果等价于 ${boolVar?c}
            // 比如 ${boolTrueVar} 将输出结果字符串 true，
            // 注意这不会影响方法参数，因为方法调用表示表达式还在计算中，非最终结果，没到最后一步的字符串渲染
            // 如 ${myFunc(boolVar)} myFunc 接受到的参数 boolVar 为 TrueTemplateBooleanModel/FalseTemplateBooleanModel
            // 当然本框架中，Function 中接收的是基本数据类型 boolean 的包装类，因为自动解包了。
            cfg.setBooleanFormat("c");
            cfg.setNumberFormat("c");
            cfg.clearTemplateCache();
        }
    }

    private static final Pattern VARIABLE_EXPRESSION = Pattern.compile("^\\$\\{([a-zA-Z\\u4e00-\\u9fa5$_][a-zA-Z\\u4e00-\\u9fa50-9$_]*)}$");
    private static final Pattern IF_TRUE_EXTRACT = Pattern.compile("^\\$\\{((?![{}])[\\S ])+}$");

    // Template 创建是个昂贵的操作，1 个 Template 占用 0.05M 内存。
    // 如果在循环中使用了表达式，那么同一个模板会创建多次，明显是不合适的，使用本地缓存改进性能。
    private static final LoadingCache<String, Template> templateCache = Caffeine.newBuilder()
        .maximumSize(2000)
        .build(key -> new Template("groot-freemarker-template", key, SingletonHolder.cfg));

    @Override
    public Object eval(Map<String, Object> model, String text) {
        if (text == null) return "null";

        // 没有表达式时无需计算，直接返回
        if (!TemplateEngine.hasExpression(text)) return text;

        // 变量表达式直接替换，使用正则，不使用 FreeMarker，如 ${username}
        // 变量名可以是 英文字母、中文字符、数字、下划线、$ 的任意组合，不能以数字开头
        // FreeMarker 用于函数表达式、嵌套对象访问、嵌套函数表达式、普通表达式(如：${random() > 3})
        if (VARIABLE_EXPRESSION.matcher(text).matches()) {
            return model.get(text.substring(2, text.length() - 1));
        }

        // 合并数据模型
        Map<String, Object> allModel = model;
        // 注册结果提取函数：如果计算结果可能为非字符串类型，需要进行结果提取
        // FreeMarker 没有提供对外 API 获取模板原始结果，除非改源码，这里通过一些技巧来实现
        boolean shouldExtract = shouldExtract(text);
        ExtractObjectHelper extractObjectHelper = null;
        if (shouldExtract) {
            extractObjectHelper = new ExtractObjectHelper();
            allModel = new HashMap<>(model);
            allModel.put(ExtractObjectHelper.NAME, extractObjectHelper);
            text = String.format("${%s(%s)}", ExtractObjectHelper.NAME, text.substring(2, text.length() - 1));
        }

        // 模板处理
        //long start = System.currentTimeMillis();
        Object res;
        try (StringWriter writer = new StringWriter()) {
            Template t = templateCache.get(text);
            t.process(allModel, writer);
            if (shouldExtract) {
                res = extractObjectHelper.getObject();
            } else {
                res = writer.toString();
            }
        } catch (Exception e) {
            throw new FreeMarkerParseException("模板 " + text + " 执行失败", e);
        }
        //System.out.printf("模板 %s 处理耗时 %d ms%n", text, System.currentTimeMillis() - start);
        return res;
    }

    private boolean shouldExtract(String template) {
        if (!(template.startsWith("${") && template.endsWith("}"))) {
            return false;
        }

        // 先用正则判断
        if (IF_TRUE_EXTRACT.matcher(template).matches()) {
            return true;
        }

        // 如果正则返回 false，则 AST 解析后判断
        Template t = templateCache.get(template);
        try {
            return Class.forName("freemarker.core.DollarVariable").isInstance(t.getRootTreeNode());
        } catch (ClassNotFoundException e) {
            throw new GrootException(e);
        }
    }

    @Override
    public Object eval(ContextWrapper ctx, String text) {
        if (text == null) return "null";

        // 没有表达式时无需计算，直接返回
        if (!TemplateEngine.hasExpression(text)) return text;

        // 注册内置函数和内置变量
        Map<String, Object> model = new HashMap<>();
        // 注册内置函数集：Functions
        ApplicationConfig.getFunctions().forEach(f -> {
            model.put(f.getName(), new FreeMarkerFunctionAdapter(ctx, f));
        });
        // 注册日志对象
        model.put(LOG.getValue(), logHelper);
        // 注册 ContextWrapper 相关对象
        if (ctx != null) {
            // 注册上下文
            model.put(CONTEXT_WRAPPER.getValue(), ctx);
            // 注册 TestCaseRunner
            Optional.ofNullable(ctx.getSessionRunner()).ifPresent(sessionRunner -> {
                model.put(SESSION_RUNNER.getValue(), sessionRunner);
            });
            // 注册变量
            Optional.ofNullable(ctx.getGlobalVariablesWrapper()).ifPresent(globalVars -> {
                model.put(GLOBAL_VARIABLES_WRAPPER.getValue(), globalVars);
            });
            Optional.ofNullable(ctx.getEnvironmentVariablesWrapper()).ifPresent(envVars -> {
                model.put(ENVIRONMENT_VARIABLES_WRAPPER.getValue(), envVars);
            });
            Optional.ofNullable(ctx.getTestVariablesWrapper()).ifPresent(testVars -> {
                model.put(TEST_VARIABLES_WRAPPER.getValue(), testVars);
            });
            Optional.ofNullable(ctx.getSessionVariablesWrapper()).ifPresent(sessionVars -> {
                model.put(SESSION_VARIABLES_WRAPPER.getValue(), sessionVars);
            });
            Optional.ofNullable(ctx.getLocalVariablesWrapper()).ifPresent(localVars -> {
                model.put(LOCAL_VARIABLES_WRAPPER.getValue(), localVars);
            });
            Optional.ofNullable(ctx.getAllVariablesWrapper()).ifPresent(allVars -> {
                model.putAll(allVars.mergeVariables());
                model.put(ALL_VARIABLES_WRAPPER.getValue(), allVars);
            });
        }
        return eval(model, text);
    }

    static class ExtractObjectHelper implements TemplateMethodModelEx {

        private static final String NAME = "groot_freemarker_extract";
        private Object object;

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            object = DeepUnwrap.unwrap((TemplateModel) arguments.get(0));
            // FreeMarker 中 ${anything} 返回 null 将抛出异常
            // 不返回 object，否则当 object 为 null 时引发报错，这里我们已经拿到原表达式计算结果，无需抛出异常
            return "name";
        }

        public Object getObject() {
            return object;
        }
    }

    public static class LogHelper {

        private static final Logger log = LoggerFactory.getLogger(LogHelper.class);

        public void trace(Object format, Object... argArray) {
            log.trace(String.valueOf(format), argArray);
        }

        public void debug(Object format, Object... argArray) {
            log.debug(String.valueOf(format), argArray);
        }

        public void info(Object format, Object... argArray) {
            log.info(String.valueOf(format), argArray);
        }

        public void warn(Object format, Object... argArray) {
            log.warn(String.valueOf(format), argArray);
        }

        public void error(Object format, Object... argArray) {
            log.error(String.valueOf(format), argArray);
        }

    }

}
