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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * 基于值的断言（值类型确定，断言类型不确定，断言类型如相等断言、数值比较、包含、正则等等）
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class MatcherAssertion<T> extends AbstractAssertion {

    private static final Logger log = LoggerFactory.getLogger(MatcherAssertion.class);
    public static final String MAPPER_KEY = "mapper";
    public static final String MAPPING_KEY = "mapping";
    public static final String MATCHERS_KEY = "matchers";

    // 从 fastjson 2.0.24 版本开始，序列化和反序列化时会忽略所有 Function 类型字段以及任何函数式接口类型的字段
    // 因为要解决 https://github.com/alibaba/fastjson2/issues/1177 问题，解决方案是否过于简单粗暴了？
    // 相关源码在 com.alibaba.fastjson2.reader.ObjectReaderCreator#createFieldReader 方法中，
    // 搜索关键字：skip function
    // 2.0.24 以上的版本，配置风格用例无法使用 mapper 字段，代码风格用例不影响
    // fastjson2 低版本和高版本都有各自的问题，本项目无法同时兼容低版本和高版本，放弃对低版本的支持，
    // 配置风格用例请使用 mapping 字段（为了保持风格统一，会自动转换 Yaml/JSON 中的 mapper 为 mapping）
    // 代码风格用例请使用 mapper 字段
    @JSONField(name = MAPPER_KEY, deserializeUsing = MapperObjectReader.class)
    protected Function<T, ?> mapper;

    // 兼容性代码：解决 fastjson2 高版本在反序列化时 mapper 字段被忽略的问题，先读取为 List，再使用第一个元素
    // 该字段仅作为配置风格用例在 fastjson2 高版本的兼容性字段使用，代码风格用例请勿使用
    @JSONField(name = MAPPING_KEY, deserializeUsing = MappingObjectReader.class)
    protected List<Function<T, ?>> mapping;

    @JSONField(name = MATCHERS_KEY)
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
                hashMap.put(MAPPER_KEY, data);
                return JSON.parseObject(JSON.toJSONString(hashMap), SequenceMapping.class);
            }
            throw new GrootException("用例格式非法，mapper 节点仅支持 string 或列表");
        }

    }

    public static class MappingObjectReader implements ObjectReader<List<Function>> {

        private static final MapperObjectReader mapperObjectReader = new MapperObjectReader();

        @Override
        public List<Function> readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            Function mapping = mapperObjectReader.readObject(jsonReader, fieldType, fieldName, features);
            if (isNull(mapping)) {
                return null;
            }

            List<Function> mappings = new ArrayList<>();
            mappings.add(mapping);
            return mappings;
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
        } else if (nonNull(mapping) && !mapping.isEmpty()) {
            actual = mapping.get(0).apply(input);
        }
        List<Class> matcherValueType = List.of(ProxyMatcher.matcherValueType(actual));

        // 计算标准 Matcher
        Matcher allOf;
        if (matchers.size() > 1) {
            List<Matcher> matchers2 = new ArrayList<>();
            for (Matcher matcher : matchers) {
                matchers2.add(ProxyMatcher.toMatcherIfProxy(ctx, matcherValueType, matcher));
            }
            allOf = Matchers.allOf((Iterable) matchers2);
        } else {
            allOf = ProxyMatcher.toMatcherIfProxy(ctx, matcherValueType, matchers.get(0));
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

    // fastjson2 2.0.24 Bug：（跟踪源码可知，高版本经测试已解决该问题）
    //
    // 低版本不兼容原因：
    // 如果字段注解上声明了 deserializeUsing，最终的 FieldReader 对应的方法是 getMapping，而非 setMapping，
    // 导致对象构建时调用的是 getMapping 方法，即 method.invoke(obj, value)，但 getMapping 方法显然没有方法参数，
    // 继续导致方法调用异常，方法参数不匹配，反序列化失败
    //
    // 该字段本来的目的是为了兼容高版本，所以提供了 Getter/Setter 方法，但是 Getter 方法在低版本又不兼容，
    // 这里可以去掉 Getter 方法来避免 Bug 导致的问题，
    // 但是项目中其他地方使用了 deserializeUsing 同时必须保留 Getter 方法（因为 fastjson2 的其他 Bug），
    // 这里去掉 Getter 方法，其他地方仍然会报错，没有意义，因此放弃对 fastjson2 低版本的支持
    //
    // 不得不吐槽几句：
    // fastjson 虽然快，但是 Bug 太折磨人了，整个项目开发过程中，fastjson 上遇到的问题最多，
    // 解决起来也费时费力，AI 和网上资源没有对应解决方案，只能 Debug，实在是太影响开发效率了，
    // 后续如果再遇到不好解决的问题，为了方便项目维护，可能考虑切换到其他 JSON 框架。。。
    public List<Function<T, ?>> getMapping() {
        return mapping;
    }

    public void setMapping(List<Function<T, ?>> mapping) {
        this.mapping = mapping;
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
