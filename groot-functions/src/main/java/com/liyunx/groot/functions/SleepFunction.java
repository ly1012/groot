package com.liyunx.groot.functions;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.GrootException;

import java.util.List;

/**
 * 等待函数，单位为毫秒
 * <p>示例：</p>
 * <ul>
 *     <li>${sleep(100)}</li>
 * </ul>
 * <p>函数参数：</p>
 * <ul>
 *     <li>等待时间，单位 ms</li>
 * </ul>
 */
public class SleepFunction extends AbstractFunction {

    @Override
    public String getName() {
        return "sleep";
    }

    @Override
    public String execute(ContextWrapper contextWrapper, List<Object> parameters) {
        checkParametersCount(parameters, 1);
        String timeInMillisAsString = String.valueOf(parameters.get(0));
        sleep(Long.parseLong(timeInMillisAsString));
        return timeInMillisAsString;
    }

    /* ------------------------------------------------------------ */
    // 直接调用，不需要实例化

    /**
     * 等待一段时间
     *
     * @param timeInMills 等待时间，单位 ms
     */
    public static void sleep(long timeInMills) {
        try {
            Thread.sleep(timeInMills);
        } catch (InterruptedException e) {
            throw new GrootException("等待函数执行失败", e);
        }
    }

}
