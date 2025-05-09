package com.liyunx.groot.processor.extractor.standard;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.processor.extractor.AbstractExtractor;

import static com.liyunx.groot.util.StringUtil.isBlank;
import static java.util.Objects.isNull;

/**
 * 标准 Extractor
 *
 * <p>标准提取元件支持位置参数写法（配置风格用例），依次为：变量名、表达式、提取参数
 */
public abstract class StandardExtractor<T> extends AbstractExtractor<T> {

    @JSONField(name = "expression")
    protected String expression;

    public StandardExtractor() {
    }

    protected StandardExtractor(Builder<?, T, ?> builder) {
        super(builder);
        this.expression = builder.expression;
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();
        if (isNull(ref) && isBlank(refName)) {
            r.append("\n提取变量 ref/refName 字段值缺失或为空，当前值：%s", toString());
        }
        if (isBlank(expression)) {
            r.append("\n提取表达式 expression 字段值缺失或为空，当前值：%s", toString());
        }
        return r;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "StandardExtractor{" +
            "expression='" + expression + '\'' +
            ", ref=" + ref +
            ", refName='" + refName + '\'' +
            ", defaultValue=" + defaultValue +
            ", scope=" + scope +
            ", disabled=" + disabled +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            '}';
    }

    //@formatter:off
    public static abstract class Builder<U extends StandardExtractor<V>,
                                         V,
                                         SELF extends Builder<U, V, SELF>>
        extends AbstractExtractor.Builder<U, V, SELF>
    //@formatter:on
    {
        private String expression;

        public SELF expression(String expression) {
            this.expression = expression;
            return self;
        }

    }

}
