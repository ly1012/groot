package com.liyunx.groot.config;

import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.common.Mergeable;
import com.liyunx.groot.common.Validatable;

/**
 * 表示配置数据（或称之为配置元件）的接口：配置项或配置组
 * <p>
 * TODO 是否有必要增加配置作用域控制属性？比如仅对当前 TestElement 生效，仅对子元件生效，对当前和子元件都生效等等。
 *
 * @param <T> 自身类型
 */
public interface ConfigElement<T> extends Validatable, Copyable<T>, Mergeable<T> {

}
