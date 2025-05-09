package com.liyunx.groot.builder;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.processor.Processor;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.nonNull;

/**
 * 用于同时支持 Lazy 和非 Lazy 模式
 *
 * @param <E> 列表元素类型
 */
public class LazyBuilder<E extends Processor> extends ArrayList<E> {

    private ContextWrapper ctx;

    @Override
    public boolean add(E e) {
        // 非 Lazy 模式，立即执行
        if (nonNull(ctx)) {
            e.process(ctx);
            return true;
        }

        // Lazy 模式，稍后执行
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        // 非 Lazy 模式，立即执行
        if (nonNull(ctx)) {
            for (E e : c) {
                e.process(ctx);
            }
            return true;
        }

        // Lazy 模式，稍后执行
        return super.addAll(c);
    }

    public void setContextWrapper(ContextWrapper ctx) {
        this.ctx = ctx;
    }

}
