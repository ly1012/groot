package com.liyunx.groot.testelement.sampler;

import com.liyunx.groot.testelement.TestResult;

/**
 * SamplerResult 抽象类，提供 Sampler 执行结果基础实现
 */
public abstract class SampleResult<T extends SampleResult<T>> extends TestResult<T> {

    // sample 时间记录。
    // 耗时不包括执行前后置处理器、模板计算等。

    /**
     * sample 开始时间 (ms)
     */
    protected long sampleStartTime;

    /**
     * sample 结束时间 (ms)
     */
    protected long sampleEndTime;

    /**
     * sample 执行耗时 (ms)
     */
    protected long sampleTime;

    // sample 时间记录，如 HTTP 请求执行前、HTTP 请求执行后

    public long getSampleStartTime() {
        return sampleStartTime;
    }

    private void setSampleStartTime(long sampleStartTime) {
        this.sampleStartTime = sampleStartTime;
    }

    public void sampleStart() {
        this.sampleStartTime = System.currentTimeMillis();
    }

    public long getSampleEndTime() {
        return sampleEndTime;
    }

    private void setSampleEndTime(long sampleEndTime) {
        if (sampleStartTime == 0)
            throw new IllegalStateException("请先设置 sampleStartTime");
        this.sampleEndTime = sampleEndTime;
        sampleTime = sampleEndTime - sampleStartTime;
    }

    public void sampleEnd() {
        if (sampleStartTime == 0)
            throw new IllegalStateException("请先设置 sampleStartTime");
        this.sampleEndTime = System.currentTimeMillis();
        sampleTime = sampleEndTime - sampleStartTime;
    }

    public long getSampleTime() {
        return sampleTime;
    }

}
