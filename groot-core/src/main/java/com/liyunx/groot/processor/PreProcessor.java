package com.liyunx.groot.processor;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.common.Validatable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.dataloader.fastjson2.deserializer.PreProcessorObjectReader;

/**
 * 前置处理器，需要保证线程安全，同一个对象可能会被多个线程共享
 *
 * <p><br>实现类需要自行调用 eval 方法完成字段值的模板计算（如果该字段支持模板）。
 * 框架无法提前计算，原因有二：
 * <ol>
 * <li> Processor 本身不能提前计算，如 HooksPreProcessor，否则会破坏原有功能 </li>
 * <li> 框架不能获知哪些字段需要被计算，哪些字段不需要被计算 </li>
 * </ol>
 */
@JSONType(deserializer = PreProcessorObjectReader.class)
@FunctionalInterface
public interface PreProcessor extends Processor, Validatable {

    /**
     * 请求执行前执行该方法
     *
     * @param ctx
     */
    void process(ContextWrapper ctx);

}
