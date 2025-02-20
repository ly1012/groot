package com.liyunx.groot;

/**
 * SessionRunner 共享数据继承
 */
public interface SessionRunnerInheritance {

    /**
     * 新 session 继承老 session 的共享数据
     *
     * @param oldSession 调用者的 session（比如当前用例）
     * @param newSession 被调用者的 session（比如当前用例中执行的引用用例）
     */
    void inheritSessionRunner(SessionRunner oldSession, SessionRunner newSession);

}
