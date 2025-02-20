package com.liyunx.groot;

import com.liyunx.groot.Groot;
import com.liyunx.groot.SessionRunner;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class AbstractGrootTestNGTestCase {

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        SessionRunner session = getGroot().newTestRunner().newSessionRunner();
        SessionRunner.setSession(session);
        session.start();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        SessionRunner session = SessionRunner.getSession();
        if (session != null) {
            session.stop();
            SessionRunner.removeSession();
        }
    }

    protected abstract Groot getGroot();

}
