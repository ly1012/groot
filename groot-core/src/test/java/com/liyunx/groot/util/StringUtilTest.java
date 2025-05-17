package com.liyunx.groot.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilTest {

    @Test(dataProvider = "urlTestData")
    public void testIsHttpOrHttps(String url, boolean expected) {
        assertThat(StringUtil.isHttpOrHttps(url)).isEqualTo(expected);
    }

    @DataProvider(name = "urlTestData")
    public Object[][] urlTestData() {
        return new Object[][]{
            {"http://www.baidu.com", true},
            {"https://www.baidu.com", true},
            // 大写
            {"HTTP://www.baidu.com", true},
            {"HTTPS://www.baidu.com", true},
            {"HttPS://www.baidu.com", true},
            // 异常用例
            {" https://www.baidu.com", false},
            {"ht tps://www.baidu.com", false},
        };
    }
}
