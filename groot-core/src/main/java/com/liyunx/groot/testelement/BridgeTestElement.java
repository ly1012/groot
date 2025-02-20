package com.liyunx.groot.testelement;

import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.support.Worker;

/**
 * 桥接元件，用于支持函数/闭包。Worker 为无参函数，避免 Java 嵌套函数时函数参数名冲突的问题。
 */
public class BridgeTestElement implements TestElement<DefaultTestResult> {

    private final Worker worker;

    public BridgeTestElement(Worker worker) {
        this.worker = worker;
    }

    @Override
    public DefaultTestResult run(SessionRunner session) {
        worker.work();
        return new DefaultTestResult();
    }

}
