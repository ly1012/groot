package com.liyunx.groot.mapping;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.dataloader.fastjson2.deserializer.MappingFunctionObjectReader;

import java.util.function.Function;

/**
 * 值映射函数
 *
 * @param <T> 函数输入值类型
 * @param <R> 函数输出值类型
 */
@JSONType(deserializer = MappingFunctionObjectReader.class)
public interface MappingFunction<T, R> extends Function<T, R> {


}
