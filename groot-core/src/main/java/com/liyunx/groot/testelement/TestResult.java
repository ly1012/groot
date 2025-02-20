package com.liyunx.groot.testelement;

import com.liyunx.groot.processor.assertion.AssertionResult;

import java.util.List;

/**
 * TestResult 抽象类，提供测试元件执行结果的基础实现
 *
 * <p>
 * <pre>
 *                 TestResult
 *                  ↑      ↑
 *  DefaultTestResult      SampleResult
 *                           ↑     ↑
 *           DefaultSampleResult  HttpSampleResult
 * </pre>
 */
public abstract class TestResult<T extends TestResult<T>> {


    // == 测试元件时间数据，耗时包括执行前后置处理器、模板计算、监听器等 ==

    /**
     * 测试元件开始执行时间 (ms)
     */
    protected long startTime;

    /**
     * 测试元件结束执行时间 (ms)
     */
    protected long endTime;

    /**
     * 测试元件执行耗时 (ms)
     */
    protected long time;

    // == 测试元件实际请求响应数据 ==

    protected RealRequest request;

    protected RealResponse response;

    // == TODO 待定 测试元件断言数据 ==

    protected List<AssertionResult> assertionResults;


    // == Getter/Setter ==

    // 测试元件时间

    public long getStartTime() {
        return startTime;
    }

    private void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void start() {
        if (startTime == 0) {
            this.startTime = System.currentTimeMillis();
        } else {
            throw new IllegalStateException("测试元件开始时间不可重复设置");
        }
    }

    public long getEndTime() {
        return endTime;
    }

    private void setEndTime(long endTime) {
        if (startTime == 0)
            throw new IllegalStateException("请先设置 startTime");
        this.endTime = endTime;
        time = endTime - startTime;
    }

    public void end() {
        if (startTime == 0)
            throw new IllegalStateException("请先设置 startTime");
        if (endTime == 0) {
            this.endTime = System.currentTimeMillis();
            time = endTime - startTime;
        } else {
            throw new IllegalStateException("测试元件结束时间不可重复设置");
        }
    }

    public long getTime() {
        return time;
    }

    // 请求/响应数据


    public RealRequest getRequest() {
        return request;
    }

    public void setRequest(RealRequest request) {
        this.request = request;
    }

    public RealResponse getResponse() {
        return response;
    }

    public void setResponse(RealResponse response) {
        this.response = response;
    }

    /**
     * {@link TestResult#then(ResultConsumer)} 语法糖
     *
     * @param name     动作名称，仅做提示用途，无实际意义
     * @param consumer 动作内容
     * @return 测试元件执行结果对象
     */
    public T then(String name, ResultConsumer<T> consumer) {
        return then(consumer);
    }

    /**
     * 消费测试元件执行结果数据
     *
     * <p>
     * 消费函数只能读取结果数据，而不应该修改结果数据。
     *
     * @param consumer 消费函数
     * @return 测试元件执行结果对象
     */
    @SuppressWarnings({"unchecked"})
    public T then(ResultConsumer<T> consumer) {
        consumer.consume((T) this);
        return (T) this;
    }

}
