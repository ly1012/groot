package com.liyunx.groot.template.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.Writer;

/**
 * 自定义的 TemplateException 处理器。
 */
public class GrootTemplateExceptionHandler implements TemplateExceptionHandler {

    @Override
    public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
        StringBuilder builder = new StringBuilder();
        String template = env.getCurrentTemplate().toString();
        if (template.contains("groot_freemarker_extract")) {
            template = String.format("${%s}", template.substring(27, template.length() - 2));
        }
        builder
            .append("\n")
            .append(te.getMessageWithoutStackTop())
            .append("\ngroot-freemarker-template：\n==> ")
            .append(template);
        throw new FreeMarkerParseException(builder.toString(), te);
    }

}
