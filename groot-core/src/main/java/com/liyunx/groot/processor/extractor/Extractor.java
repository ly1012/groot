package com.liyunx.groot.processor.extractor;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.dataloader.fastjson2.deserializer.ExtractorObjectReader;
import com.liyunx.groot.processor.PostProcessor;

/**
 * 提取元件，后置元件的一种
 *
 * <p>默认实现规范，以保证不同实现下提取行为的统一：
 * <ul>
 *     <li>提取失败，应当抛出提取异常：异常信息中说明提取失败原因</li>
 *     <li>提取配置项中有抑制异常开关：决定提取失败时，是否抛出异常（待定）</li>
 *     <li>提取失败，在抛出异常前，仍需保存一个表示提取失败的特定常量值到变量，防止开启抑制异常时发生 NPE（待定）</li>
 * </ul>
 */
@JSONType(deserializer = ExtractorObjectReader.class)
@FunctionalInterface
public interface Extractor extends PostProcessor {

    String EXTRACT_FAILURE_VALUE = "__extract_failure__";

    /**
     * 执行提取动作
     */
    void process(ContextWrapper ctx);

}
