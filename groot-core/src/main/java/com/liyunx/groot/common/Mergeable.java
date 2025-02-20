package com.liyunx.groot.common;

public interface Mergeable<T> {

    /**
     * 合并相同类型的对象，参数对象的值会覆盖当前对象的值，方法应返回一个新的对象。
     *
     * @param other 新的对象
     * @return 合并后的对象
     */
    T merge(T other);

}
