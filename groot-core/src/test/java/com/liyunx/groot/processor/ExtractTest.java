package com.liyunx.groot.processor;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.processor.extractor.ExtractScope;
import com.liyunx.groot.support.Ref;
import org.testng.annotations.Test;

import java.util.Objects;

import static com.liyunx.groot.DefaultVirtualRunner.noopWith;
import static com.liyunx.groot.DefaultVirtualRunner.sv;
import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.support.Ref.ref;

public class ExtractTest extends GrootTestNGTestCase {

    @Test(description = "标准写法测试")
    public void test1_1() {
        getSession().run("testcases/processor/extract1_1.yml");
    }

    @Test(description = "位置参数写法测试")
    public void test1_2() {
        getSession().run("testcases/processor/extract1_2.yml");
    }

    @Test(description = "提取作用域测试")
    public void test1_3() {
        getSession().run("testcases/processor/extract1_3.yml");
    }

    @Test
    public void testJava_1() {
        noopWith("refName 写法", noop -> noop
            .extract(extract -> extract
                .jsonpath("id", "$.id", params -> params.target("{\"id\": \"abc\"}")))
            .validate(validate -> validate
                .equalTo("${id}", "abc")));
    }

    @Test
    public void testJava_1_1() {
        noopWith("refName 写法", noop -> noop
            .extract(extract -> extract
                .jsonpath("id", "$.id", params -> params.target("{\"id\": \"abc\"}").scope(ExtractScope.SESSION)))
            .validate(validate -> validate
                .equalTo("${id}", "abc")));
        assert Objects.equals(sv("id"), "abc");
    }

    @Test
    public void testJava_2() {
        Ref<String> id = ref();
        noopWith("ref 写法", noop -> noop
            .extract(extract -> extract
                .jsonpath(id, "$.id", params -> params.target("{\"id\": \"abc\"}")))
            .validate(validate -> validate
                .equalTo(id.value, "abc")));
        assert Objects.equals(id.value, "abc");
    }


}
