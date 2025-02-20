package com.liyunx.groot.filter;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.dataloader.fastjson2.deserializer.TestFilterObjectReader;

/**
 * TestElement 生命周期切面（采用 Filter 模式实现）。
 *
 * <p>这里没有采用监听器实现，因为监听器是先执行所有 before 事件，再执行所有 after 事件，
 * 并且无法很好的应对异常处理，不满足测试元件额外逻辑的需求。
 *
 * <p>当前已知的额外逻辑需求：
 * <ul>
 *     <li>Allure 报告集成</li>
 *     <li>协议请求的加解密、授权认证等通用逻辑</li>
 * </ul>
 *
 * <p>TestFilter 和 Processor 的区别：
 * <ul>
 *   <li>Processor: 对当前测试元件的前后置处理，不会作用于子元件。</li>
 *   <li>TestFilter：对作用域下所有测试元件的通用前后置处理，如报告数据采集、接口加解密、自动登录与认证维持等等。</li>
 * </ul>
 *
 * <p>执行顺序：
 * <pre><code>
 *     doRun ==>
 *         // 变量配置项中的表达式计算
 *         // 前置处理器 setupBefore
 *         doExecute ==>
 *             doExecuteSubSteps ==>
 *                 // handleRequest
 *                 // 前置处理器 setupAfter
 *                 doSample ==>
 *                 doSample <==
 *                 // handleResponse
 *             doExecuteSubSteps <==
 *         doExecute <==
 *         // 后置处理器
 *     doRun <==
 * </code></pre>
 * <p>
 */
@JSONType(deserializer = TestFilterObjectReader.class)
public interface TestFilter {

    /**
     * 计算当前测试元件对象是否需要应用该过滤器
     *
     * @param ctx         测试上下文
     * @return 如果对 testElement 使用当前过滤器则为 true，否则为 false。
     */
    default boolean match(ContextWrapper ctx) {
        return false;
    }

    /**
     * 执行测试元件时调用
     *
     * @param ctx
     * @param chain
     */
    default void doRun(ContextWrapper ctx, RunFilterChain chain) {
        chain.doRun(ctx);
    }

    /**
     * 执行测试元件具体功能逻辑时调用（前后置处理器不在内）
     *
     * @param ctx
     * @param chain
     */
    default void doExecute(ContextWrapper ctx, ExecuteFilterChain chain) {
        chain.doExecute(ctx);
    }

    /**
     * 执行某次循环时调用
     *
     * @param ctx
     * @param chain
     */
    default void doExecuteSubSteps(ContextWrapper ctx, ExecuteSubStepsFilterChain chain) {
        chain.doExecuteSubSteps(ctx);
    }

    /**
     * 执行取样逻辑时调用（一般为协议请求）
     *
     * @param ctx
     * @param chain
     */
    default void doSample(ContextWrapper ctx, SampleFilterChain chain) {
        chain.doSample(ctx);
    }

}
