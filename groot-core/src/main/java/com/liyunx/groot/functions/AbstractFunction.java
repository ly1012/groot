package com.liyunx.groot.functions;

import com.liyunx.groot.context.ContextWrapper;

import java.util.List;

/**
 * 提供函数的一些通用方法。
 */
public abstract class AbstractFunction implements Function {

    // --------------------------------------------------
    // 参数检查
    // --------------------------------------------------

    /**
     * 上下文对象非空检查。
     *
     * @param contextWrapper 上下文对象
     * @throws UnsupportedOperationException contextWrapper 为 null 时抛出该异常
     */
    protected void requireNotNull(ContextWrapper contextWrapper) {
        if (contextWrapper == null) {
            throw new UnsupportedOperationException(
                String.format("%s 函数依赖上下文对象，ContextWrapper 参数不能为 null", getName())
            );
        }
    }

    /**
     * 参数数量检查。
     *
     * @param parameters 参数集合
     * @param min        允许的最小参数数量
     * @param max        允许的最大参数数量
     * @throws IllegalArgumentException 如果参数数量不符合函数要求，则抛出该异常
     */
    protected void checkParametersCount(List<Object> parameters, int min, int max) {
        int num = parameters.size();
        if (num > max || num < min) {
            String expected = min == max ? String.valueOf(min) : "[" + min + ", " + max + "]";
            String msg = String.format("%s 函数参数数量错误，实际参数数量：%d，预期参数数量：%s", getName(), num, expected);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * 参数数量检查。
     *
     * @param parameters 参数集合
     * @param count      预期的参数数量
     * @throws IllegalArgumentException 如果参数数量不符合函数要求，则抛出该异常
     */
    protected void checkParametersCount(List<Object> parameters, int count) {
        int num = parameters.size();
        if (num != count) {
            String msg = String.format("%s 函数参数数量错误，实际参数数量：%d，预期参数数量：%d", getName(), num, count);
            throw new IllegalArgumentException(msg);
        }
    }

}
