package com.liyunx.groot.processor;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * HooksProcessor 抽象类
 */
public abstract class AbstractHooksProcessor extends AbstractProcessor {

    private static final Logger log = LoggerFactory.getLogger(AbstractHooksProcessor.class);

    public static final String KEY = "hooks";

    @JSONField(name = "hooks")
    protected List<String> hooks;

    public AbstractHooksProcessor() {
    }

    protected AbstractHooksProcessor(Builder<?, ?> builder) {
        super(builder);
        this.hooks = builder.hooks;
    }

    public ValidateResult validate() {
        ValidateResult r = new ValidateResult();
        if (hooks != null) {
            hooks.forEach(hook -> {
                if (!TemplateEngine.hasExpression(hook)) {
                    r.append(
                        "\n字段：setup/teardown.hooks，前置/后置函数处理器应该包含插值表达式，如 ${func(param1, param2)}，当前值：%s",
                        hook);
                }
            });
        }
        return r;
    }

    public void process(ContextWrapper ctx, String debugMessage) {
        if (hooks != null) {
            TemplateEngine templateEngine = ctx.getSessionRunner().getConfiguration().getTemplateEngine();
            hooks.forEach(hook -> {
                log.info("执行{}函数：{}", debugMessage, hook);
                templateEngine.eval(ctx, hook);
            });
        }
    }

    @Override
    public String name() {
        return name != null ? name : "Hooks";
    }

    public List<String> getHooks() {
        return hooks;
    }

    public void setHooks(List<String> hooks) {
        this.hooks = hooks;
    }

    public static abstract class Builder<T, SELF extends Builder<T, SELF>>
        extends AbstractProcessor.Builder<T, SELF> {

        private List<String> hooks = new ArrayList<>();

        public SELF hook(String hook) {
            hooks.add(hook);
            return self;
        }

        public SELF hooks(List<String> hooks) {
            this.hooks = hooks;
            return self;
        }

    }

}
