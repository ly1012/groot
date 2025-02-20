package com.liyunx.groot;

public interface GrootListener {

    /**
     * Groot 开始时调用，执行初始化动作
     */
    void grootStart(Groot groot);

    /**
     * Groot 结束时调用，执行清理动作
     */
    void grootStop(Groot groot);

}
