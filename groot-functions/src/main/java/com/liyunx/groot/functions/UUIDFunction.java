package com.liyunx.groot.functions;

import com.liyunx.groot.context.ContextWrapper;

import java.util.List;
import java.util.UUID;

/**
 * 生成 UUID 字符串
 */
public class UUIDFunction extends AbstractFunction {

    @Override
    public String getName() {
        return "uuid";
    }

    @Override
    public String execute(ContextWrapper contextWrapper, List<Object> parameters) {
        checkParametersCount(parameters, 0);
        return randomUUID();
    }

    // ---------------------------------------------------------------------
    // 直接调用，不需要实例化
    // ---------------------------------------------------------------------

    /**
     * 随机 UUID 字符串
     *
     * @return 随机 UUID 字符串
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

}
