package com.liyunx.groot.common;

/**
 * 表示一个类是有序的，一般用于 Listener 或 Filter 等场景。
 *
 * <p>默认值为 1000，值越小排序越靠前，取值范围为 [Integer.MIN_VALUE, Integer.MAX_VALUE]。
 */
public interface Ordered {

    /**
     * 默认值
     */
    int DEFAULT_PRECEDENCE = 1000;

    /**
     * 最高值（最小值，排名最前）
     */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * 最低值（最大值，排名最后）
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * @return 排序时的值
     */
    int getOrder();

}
