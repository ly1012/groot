package com.liyunx.groot.util;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

public class TimeWatchTest {

    @Test
    public void testStart() throws InterruptedException {
        TimeWatch watch = new TimeWatch();
        watch.start();
        Thread.sleep(1);
        long time = watch.stopAndGetTime();
        assertTrue(time > 0);
    }

    @Test
    public void testStopAndPrint() {
        TimeWatch watch = new TimeWatch();
        watch.start();
        watch.stopAndPrint("SPI");
    }

    @Test
    public void testStopAndPrintWith() throws InterruptedException {
        TimeWatch watch = new TimeWatch();
        watch.start();
        Thread.sleep(1);
        watch.stopAndPrintWith("SPI 加载时间：{} ms");
    }

    @Test
    public void testStopAndGetMessage() {
        TimeWatch watch = new TimeWatch();
        watch.start();
        assertThat(watch.stopAndGetMessage("SPI")).contains("SPI耗时：");
    }

}