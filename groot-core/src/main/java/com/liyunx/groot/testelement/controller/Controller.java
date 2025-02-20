package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.TestResult;

/**
 * 控制器标记接口，表示一个测试元件是逻辑控制元件，即负责调度具体的请求，而不负责具体请求的执行。
 */
public interface Controller<T extends TestResult<T>>
    extends TestElement<T> {

}
