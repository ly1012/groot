package com.liyunx.groot.mapping;

import com.liyunx.groot.mapping.internal.InternalSequenceMapping;

import java.util.function.Function;

/**
 * 试验性质，不够智能，缺少输入输出值类型提示
 */
@Deprecated
public class MappingsBuilder<T, R> {

    private final InternalSequenceMapping<T, R> mappings = new InternalSequenceMapping<>();

    public static <T, R> MappingsBuilder<T, R> mappings() {
        return new MappingsBuilder<>();
    }

    public MappingsBuilder<T, R> toInt() {
        mappings.addMapper(IntMapping.INT_MAPPING);
        return this;
    }

    public MappingsBuilder<T, R> toLong() {
        mappings.addMapper(LongMapping.LONG_MAPPING);
        return this;
    }

    public MappingsBuilder<T, R> toStr() {
        mappings.addMapper(StringMapping.STRING_MAPPING);
        return this;
    }

    public MappingsBuilder<T, R> map(Function<?, ?> mapper) {
        mappings.addMapper(mapper);
        return this;
    }

    public InternalSequenceMapping<T, R> build() {
        return mappings;
    }

}
