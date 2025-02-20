package com.liyunx.groot.common;

import com.liyunx.groot.util.KryoUtil;

/**
 * 表示一个类是可复制的，至于是浅拷贝、部分浅拷贝，还是深拷贝取决于具体的类。
 *
 * @param <T> 可复制类的类型
 */
public interface Copyable<T> {

    T copy();

    default T deepCopy(T object) {
        return KryoUtil.copy(object);
    }

}
