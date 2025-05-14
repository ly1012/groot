package com.liyunx.groot.processor.assertion.standard;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.context.ContextWrapper;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 相等断言
 */
@KeyWord(EqualToAssertion.KEY)
public class EqualToAssertion extends StandardAssertion<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(EqualToAssertion.class);

    public static final String KEY = "equalTo";

    /**
     * [String] 忽视大小写
     */
    private boolean ignoreCase = false;

    // SPI 扫描需要无参构造器
    public EqualToAssertion() {
    }

    public EqualToAssertion(Object actualValue, Object expectedValue) {
        super(actualValue, expectedValue);
    }

    private EqualToAssertion(Builder builder) {
        super(builder);
        this.ignoreCase = builder.ignoreCase;
    }

    @Override
    public void process(ContextWrapper ctx) {
        log.info("相等断言，{}实际值：{}，预期值：{}，忽视大小写（String）：{}",
            name == null ? "" : name + "，",
            actualValue, expectedValue, ignoreCase);
        // 计算表达式
        Object actual = ctx.evalIfString(actualValue);
        Object expected = ctx.evalIfString(expectedValue);

        // 实际值预期值类型一致性检查
        typeCheck(actual, expected);

        // String 相等断言
        if (actual instanceof String) {
            AbstractStringAssert<?> stringAssert = Assertions.assertThat((String) actual);
            if (ignoreCase) {
                stringAssert.isEqualToIgnoringCase((String) expected);
            } else {
                stringAssert.isEqualTo(expected);
            }
            log.info("断言成功，实际值：{}，预期值：{}", actual, expected);
            return;
        }

        // 其他类型断言
        Assertions.assertThat(actual).isEqualTo(expected);
        log.info("断言成功，实际值：{}，预期值：{}", actual, expected);
    }

    private void typeCheck(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return;
        }
        Class<?> aClass = actual == null ? null : actual.getClass();
        Class<?> eClass = expected == null ? null : expected.getClass();
        if (aClass == null || !aClass.equals(eClass)) {
            throw new AssertionError(String.format("实际值和预期值类型不一致，实际值：%s(%s)，预期值：%s(%s)",
                actual,
                aClass == null ? null : aClass.getName(),
                expected,
                eClass == null ? null : eClass.getName()));
        }
    }

    @Override
    public String name() {
        return name == null ? "相等断言" : name;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public static class Builder extends StandardAssertion.Builder<EqualToAssertion, Object, Object, Builder> {

        private boolean ignoreCase;

        /**
         * 忽略大小写
         */
        public Builder ignoreCase() {
            ignoreCase = true;
            return this;
        }

        /**
         * 忽略大小写
         *
         * @param ignoreCase true 忽略大小写，否则不忽略
         */
        public Builder ignoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            return this;
        }

        @Override
        public EqualToAssertion build() {
            return new EqualToAssertion(this);
        }

    }

}


