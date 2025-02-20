package com.liyunx.groot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * 计时工具
 *
 * <p>
 * 该类非线程安全类，每个线程中应重新创建对象。
 */
public class TimeWatch {

    private static final Logger log = LoggerFactory.getLogger(TimeWatch.class);

    private long startTime;

    /**
     * 开始计时
     */
    public void start() {
        startTime = System.currentTimeMillis();
    }

    /**
     * 停止计时，并打印日志
     *
     * @param watchName 计时目标名称，如 SPI 扫描
     */
    public void stopAndPrint(String watchName) {
        stopAndPrintWith(watchName + "：{} ms");
    }

    /**
     * 停止计时，并打印日志
     *
     * @param message 模板字符串，接受一个 {} 参数，值为耗时（ms）
     */
    public void stopAndPrintWith(String message) {
        //log.info(message, stopAndGetTime());
        System.out.println(message.replace("{}", String.valueOf(stopAndGetTime())));
    }

    /**
     * 获取耗时（ms）
     *
     * @return 耗时（ms）
     */
    public long stopAndGetTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取耗时信息
     *
     * @param watchName 计时目标名称，如 SPI 扫描
     * @return 耗时信息
     */
    public String stopAndGetMessage(String watchName) {
        return MessageFormat.format(watchName + "耗时：{0} ms", stopAndGetTime());
    }

}
