package com.liyunx.groot.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@Deprecated
@SuppressWarnings({"rawtypes", "unchecked"})
public class InternalSequenceMapping<T, R> implements Function<T, R> {

    private List<Function> mappings = new ArrayList<>();

    @Override
    public Object apply(Object input) {
        if (mappings == null || mappings.isEmpty()) {
            return input;
        }

        Object r = input;
        for (Function mapping : mappings) {
            r = mapping.apply(r);
        }
        return r;
    }

    public void addMapper(Function mapper) {
        mappings.add(mapper);
    }

}
