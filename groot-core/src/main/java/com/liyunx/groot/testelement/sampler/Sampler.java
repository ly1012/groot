package com.liyunx.groot.testelement.sampler;

import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.controller.AbstractContainerController;
import com.liyunx.groot.testelement.controller.Controller;

/**
 * Sampler 接口，表示一个测试元件是最基本的测试执行单元，其下没有子元件。
 *
 * <p>Sampler 一般是各种协议请求实现，如 JDBC 请求、HTTP 请求、Dubbo 请求等等，
 * 或者是最基本的动作，如打开一个网页、点击一个按钮等等。
 *
 * <p>如果是控制逻辑，而非直接的请求执行，如各种逻辑控制器、用例引入步骤，可以使用 {@link Controller}。
 * {@link AbstractContainerController} 提供了子元件调度的基础实现。
 */
public interface Sampler<T extends SampleResult<T>>
    extends TestElement<T> {


}
