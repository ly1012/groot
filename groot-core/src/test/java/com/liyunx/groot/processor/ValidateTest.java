package com.liyunx.groot.processor;

import com.liyunx.groot.GrootTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.noopWith;
import static com.liyunx.groot.SessionRunner.getSession;

public class ValidateTest extends GrootTestNGTestCase {

    @Test(description = "标准写法测试")
    public void test1_1() {
        getSession().run("testcases/processor/validate1_1.yml");
    }

    @Test(description = "位置参数写法测试")
    public void test1_2() {
        getSession().run("testcases/processor/validate1_2.yml");
    }

    @Test
    public void testJava_1() {
        noopWith("标准写法", noop -> noop
            .validate(validate -> validate
                .equalTo("abc", "ABC", params -> params.ignoreCase())));
    }

}
