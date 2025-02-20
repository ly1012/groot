package com.liyunx.groot.processor.assertion.matchers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.GrootException;
import com.liyunx.groot.mapping.MappingFunction;
import com.liyunx.groot.mapping.SequenceMapping;
import com.liyunx.groot.matchers.ProxyMatcher;
import com.liyunx.groot.processor.assertion.AbstractAssertion;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

/**
 * 基于值的断言（值类型确定，断言类型不确定，断言类型如相等断言、数值比较、包含、正则等等）
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class MatcherAssertion<T> extends AbstractAssertion {

    private static final Logger log = LoggerFactory.getLogger(MatcherAssertion.class);

    @JSONField(name = "mapper", deserializeUsing = MapperObjectReader.class)
    protected Function<T, ?> mapper;

    @JSONField(name = "matchers")
    protected List<Matcher> matchers;

    public static class MapperObjectReader implements ObjectReader<MappingFunction> {

        @Override
        public MappingFunction readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            Object mapperData = jsonReader.readAny();
            if (mapperData instanceof String) {
                // 注意 mapperData 需要加双引号，表示 JSON 的字符串类型值，否则会引发 JSON 解析失败
                return JSON.parseObject(String.format("\"%s\"", mapperData), MappingFunction.class);
            }
            if (mapperData instanceof List data) {
                HashMap<String, List> hashMap = new HashMap<>();
                hashMap.put("mapper", data);
                return JSON.parseObject(JSON.toJSONString(hashMap), SequenceMapping.class);
            }
            throw new GrootException("用例格式非法，mapper 节点仅支持 string 或列表");
        }

    }

    public MatcherAssertion() {
    }

    protected MatcherAssertion(Builder<?, T, ?> builder) {
        super(builder);
        this.mapper = builder.mapper;
        this.matchers = builder.matchers;
    }

    protected MatcherAssertion(Matcher matcher, Matcher... extraMatchers) {
        this.matchers = new ArrayList<>();
        this.matchers.add(matcher);
        if (extraMatchers.length > 0) {
            this.matchers.addAll(Arrays.asList(extraMatchers));
        }
    }

    protected MatcherAssertion(Function<T, ?> mapper, Matcher matcher, Matcher... extraMatchers) {
        this(matcher, extraMatchers);
        this.mapper = mapper;
    }

    @Override
    public final void process(ContextWrapper ctx) {
        if (matchers == null || matchers.isEmpty()) {
            throw new IllegalStateException("MatcherAssertion 预期值为空，至少需要一个 Matcher 对象");
        }

        // 初始化当前线程数据
        ProxyMatcher.ctxThreadLocal.set(ctx);
        ProxyMatcher.matcherThreadLocal.set(new HashMap<>());

        // 计算实际值
        T input = extractInitialValueOfActual(ctx);
        Object actual = input;
        if (mapper != null) {
            actual = mapper.apply(input);
        }

        // 计算标准 Matcher
        Matcher allOf;
        if (matchers.size() > 1) {
            List<Matcher> matchers2 = new ArrayList<>();
            for (Matcher matcher : matchers) {
                matchers2.add(toMatcherIfProxy(ctx, actual, matcher));
            }
            allOf = Matchers.allOf((Iterable) matchers2);
        } else {
            allOf = toMatcherIfProxy(ctx, actual, matchers.get(0));
        }

        // 执行断言逻辑
        matcherAssert(ctx, actual, allOf);
        StringDescription description = new StringDescription();
        allOf.describeTo(description);
        log.info("断言成功，初始值：{}，校验值：{}，断言内容：{}",
            input,
            Objects.equals(input, actual) ? "和初始值相等" : actual,
            description);

        // 清理当前线程数据
        ProxyMatcher.matcherThreadLocal.remove();
        ProxyMatcher.ctxThreadLocal.remove();
    }

    private Matcher toMatcherIfProxy(ContextWrapper ctx, Object actual, Matcher matcher) {
        if (matcher instanceof ProxyMatcher proxyMatcher) {
            Class<?> matcherValueType = matcherValueType(actual);
            // 仅当没有指定预期值类型，且存在默认预期值类型的情况下，才传递预期值类型
            if (proxyMatcher.getMatcherValueType() == null && matcherValueType != null) {
                proxyMatcher.setMatcherValueType(matcherValueType);
            }
            return proxyMatcher.toMatcher(ctx);
        } else {
            return matcher;
        }
    }

    /**
     * 根据实际值计算默认预期值类型
     *
     * @return 默认预期值类型，可能值：基本类型、String 类型、null
     */
    private Class<?> matcherValueType(Object actual) {
        // 使用频率高的先判断
        // String 类型
        if (actual instanceof String)
            return String.class;

        // 基本数据类型
        if (actual instanceof Integer)
            return Integer.class;

        if (actual instanceof Long)
            return Long.class;

        if (actual instanceof Double)
            return Double.class;

        if (actual instanceof Boolean)
            return Boolean.class;

        if (actual instanceof Byte)
            return Byte.class;

        if (actual instanceof Short)
            return Short.class;

        if (actual instanceof Float)
            return Float.class;

        if (actual instanceof Character)
            return Character.class;

        // 其他类型，不支持默认预期值类型
        return null;
    }

    // ---------------------------------------------------------------------
    // 子类可能需要重写的方法
    // ---------------------------------------------------------------------

    /**
     * 提取实际值的初始值
     *
     * <p>实际值：初始值(extractInitialValueOfActual) -> 最终值(mapper)
     *
     * @param ctx 上下文对象
     * @return 实际值的初始值
     */
    protected abstract T extractInitialValueOfActual(ContextWrapper ctx);

    /**
     * 执行具体的断言逻辑，子类可按需重写
     *
     * @param ctx     上下文对象
     * @param matcher 预期结果
     */
    protected void matcherAssert(ContextWrapper ctx, Object actual, Matcher matcher) {
        MatcherAssert.assertThat(actual, matcher);
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public Function<T, ?> getMapper() {
        return mapper;
    }

    public void setMapper(Function<T, ?> mapper) {
        this.mapper = mapper;
    }

    public List<Matcher> getMatchers() {
        return matchers;
    }

    public void setMatchers(List<Matcher> matchers) {
        this.matchers = matchers;
    }

    //@formatter:off
    public static abstract class Builder<U extends MatcherAssertion<T>,
                                         T,
                                         SELF extends Builder<U, T, SELF>>
        extends AbstractAssertion.Builder<U, SELF>
    //@formatter:on
    {
        private Function<T, ?> mapper;
        private List<Matcher> matchers;

        public SELF mapper(Function<T, ?> mapper) {
            this.mapper = mapper;
            return self;
        }

        public SELF matchers(Matcher matcher, Matcher... extraMatchers) {
            this.matchers = new ArrayList<>();
            this.matchers.add(matcher);
            if (extraMatchers.length > 0) {
                this.matchers.addAll(Arrays.asList(extraMatchers));
            }
            return self;
        }

    }

}
