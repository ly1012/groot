package com.liyunx.groot.processor.extractor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.config.builtin.ExtractConfigItem;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.model.TestStatus;
import com.liyunx.groot.processor.AbstractProcessor;
import com.liyunx.groot.support.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供通用字段和通用方法
 * <p>
 * Extractor 配置用例 Map 形式书写，仅支持 AbstractExtractor，变量名对应 name 字段。
 *
 * @param <T> 默认值类型
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractExtractor<T> extends AbstractProcessor implements Extractor {

    private static final Logger log = LoggerFactory.getLogger(AbstractExtractor.class);

    // Ref 的使用不是线程安全的，不要共用 Ref 对象，其应用场景为函数式风格用例
    protected Ref ref;

    @JSONField(name = "refName")
    protected String refName;

    @JSONField(name = "default")
    protected T defaultValue;

    @JSONField(name = "scope")
    protected ExtractScope scope;

    public AbstractExtractor() {
    }

    protected AbstractExtractor(Builder<?, T, ?> builder) {
        super(builder);
        if (builder.ref == null
            && (builder.refName == null || builder.refName.trim().isEmpty())) {
            throw new IllegalArgumentException("Extractor variable name cannot be empty");
        }

        this.ref = builder.ref;
        this.refName = builder.refName;
        this.defaultValue = builder.defaultValue;
        this.scope = builder.scope;
    }

    @Override
    public void process(ContextWrapper ctx) {
        ExtractResult res = extract(ctx);

        if (TestStatus.PASSED.equals(res.getStatus())) {
            log.info("提取成功，值：{}", JSON.toJSONString(res.getValue()));
            saveExtractDataToRefAndScopeVariable(ctx, res.getValue());
            return;
        }

        // 默认值仅在没有提取到数据时生效，如果是其他原因导致的失败，当作提取失败处理
        // 默认值不允许为 null，null 表示无默认值
        // 若要改造成任何原因导致失败都使用默认值，需要去掉状态判断，
        // 并 catch extract(ctx) 方法调用产生的异常，设置 res.setStatus(BROKEN)
        // 若要同时支持两种方式，需要增加一个字段表示默认值使用策略
        if (TestStatus.FAILED.equals(res.getStatus()) && defaultValue != null) {
            log.info("提取失败，使用默认值：{}", JSON.toJSONString(defaultValue));
            res.setStatus(TestStatus.PASSED);
            saveExtractDataToRefAndScopeVariable(ctx, defaultValue);
            return;
        }

        saveExtractDataToRefAndScopeVariable(ctx, EXTRACT_FAILURE_VALUE);
        throw new GrootException(res.getMessage(), res.getException());
    }

    /**
     * 执行提取动作
     *
     * @param ctx 上下文
     * @return 提取结果
     */
    protected abstract ExtractResult extract(ContextWrapper ctx);

    // 根据指定作用域，保存提取数据到指定变量或指定变量名
    protected void saveExtractDataToRefAndScopeVariable(ContextWrapper ctx, Object value) {
        if (ref != null) {
            ref.value = value;
        }

        if (refName == null) {
            return;
        }

        ExtractScope scope = resolveScope(ctx);
        switch (scope) {
            case LOCAL:
                ctx.getLocalVariablesWrapper().put(refName, value);
                break;
            case ALL:
                ctx.getAllVariablesWrapper().put(refName, value);
                break;
            case SESSION:
                ctx.getSessionVariablesWrapper().put(refName, value);
                break;
            case TEST:
                ctx.getTestVariablesWrapper().put(refName, value);
                break;
            case ENVIRONMENT:
                ctx.getEnvironmentVariablesWrapper().put(refName, value);
                break;
            case GLOBAL:
                ctx.getGlobalVariablesWrapper().put(refName, value);
                break;
        }
    }

    // 计算提取作用域：
    // 最近优先原则，当前提取器 -> 所属测试元件上下文 -> 父测试元件上下文* -> 环境上下文 -> 全局上下文
    private ExtractScope resolveScope(ContextWrapper ctx) {
        if (this.scope != null) {
            return this.scope;
        }

        ExtractScope scope;
        ExtractConfigItem extractConfigItem = ctx.getConfigGroup().get(ExtractConfigItem.KEY);
        if (extractConfigItem != null && (scope = extractConfigItem.getScope()) != null) {
            return scope;
        }

        return ExtractScope.ALL;
    }

    public Ref getRef() {
        return ref;
    }

    public void setRef(Ref ref) {
        this.ref = ref;
    }

    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ExtractScope getScope() {
        return scope;
    }

    public void setScope(ExtractScope scope) {
        this.scope = scope;
    }


    //@formatter:off
    @SuppressWarnings("rawtypes")
    public static abstract class Builder<U extends AbstractExtractor<V>,
                                         V,
                                         SELF extends Builder<U, V, SELF>>
        extends AbstractProcessor.Builder<U, SELF>
    //@formatter:on
    {

        protected Ref ref;
        protected String refName;
        protected V defaultValue;
        protected ExtractScope scope;

        public SELF ref(Ref ref) {
            this.ref = ref;
            return self;
        }

        public SELF refName(String name) {
            this.refName = name;
            return self;
        }

        public SELF defaultValue(V defaultValue) {
            this.defaultValue = defaultValue;
            return self;
        }

        public SELF scope(ExtractScope scope) {
            this.scope = scope;
            return self;
        }

    }

}
