package com.liyunx.groot.testng.annotation;

import com.liyunx.groot.testng.GrootTestNGTestCase;
import com.liyunx.groot.testng.annotation.GrootDataSource;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GrootDataSourceTest extends GrootTestNGTestCase {

    @GrootDataSource(
        value = "data/test.csv",
        slice = "[1..-1]", expr = "username == 'admin' && password == '654321'")
    @Test(description = "测试 @GrootDataSource 和 $(\"key\") 方法")
    public void testGrootDataSource(Map<String, Object> arg) {
        login((String) arg.get("username"), Integer.parseInt((String) arg.get("password")));

        // 使用 arg("username") 代替 (String) arg.get("username")
        login(arg("username"), Integer.parseInt(arg("password")));
    }

    private void login(String username, int password) {
        assertThat(username).isEqualTo("admin");
        assertThat(password).isEqualTo(654321);
    }

    @GrootDataSource(value = "data/test.csv", parallel = true)
    @Test(description = "并发测试", invocationCount = 200, threadPoolSize = 10)
    public void testParallel(Map<String, Object> arg) {
        login(arg("username"), arg("password"));
    }

    private void login(String username, String password) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
