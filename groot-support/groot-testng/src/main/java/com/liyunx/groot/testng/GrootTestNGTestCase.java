package com.liyunx.groot.testng;

import com.liyunx.groot.Groot;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Groot TestNG 测试用例基类，全局使用同一个 Groot 实例
 *
 * <p>如果有其他需求，可以继承 {@link AbstractGrootTestNGTestCase} 实现自定义的子类</p>
 */
public class GrootTestNGTestCase extends AbstractGrootTestNGTestCase {

    protected static Groot groot;

    @BeforeSuite
    @Parameters({"environment"})
    public void startGroot(@Optional String environment) {
        groot = new Groot(environment);
        groot.start();
    }

    @Override
    protected Groot getGrootInstance() {
        return groot;
    }

    @AfterSuite(alwaysRun = true)
    public void stopGroot() {
        if (groot != null) {
            groot.stop();
        }
    }

}
