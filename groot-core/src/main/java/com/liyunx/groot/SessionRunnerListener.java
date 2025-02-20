package com.liyunx.groot;

public interface SessionRunnerListener {

    /**
     * SessionRunner 开始时调用，执行初始化动作
     * @param sessionRunner
     */
    void sessionRunnerStart(SessionRunner sessionRunner);

    /**
     * SessionRunner 结束时调用，执行清理动作
     */
    void sessionRunnerStop(SessionRunner sessionRunner);

}
