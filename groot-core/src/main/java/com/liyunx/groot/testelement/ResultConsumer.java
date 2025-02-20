package com.liyunx.groot.testelement;

/**
 * TestResult 数据消费接口
 *
 * @param <T> 具体的 TestResult 类，如 HttpSampleResult
 */
@FunctionalInterface
public interface ResultConsumer<T extends TestResult<T>> {

    /**
     * 消费测试元件执行结果数据
     *
     * <p>代码示例：<pre><code>
     * $.$http("Get 请求", it -> {
     *   it.get("https://httpbin.org/get");
     * }).then(r -> {
     *   System.out.println(r.getTime());
     * });
     * </code></pre>
     *
     * @param r 测试元件执行结果
     */
    void consume(T r);

}
