package com.liyunx.groot.processor;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.dataloader.DataLoadException;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.noopWith;
import static com.liyunx.groot.SessionRunner.getSession;

public class TeardownTest extends GrootTestNGTestCase {

    @Test(description = "teardown/extract/validate 同级")
    public void testByJava() {
        String json = """
            {"id": "abc"}
            """;
        noopWith("同级写法", noop -> noop
            .teardown(teardown -> teardown
                .hooks("${vars.put('x', 1)}"))
            .extract(extract -> extract
                .jsonpath("id", "$.id", params -> params.target(json)))
            .validate(validate -> validate
                .equalTo("${x?int}", 1)
                .equalTo("${id}", "abc")));
    }

    @Test(description = "extract 和 validate 位于 teardown 中")
    public void test2ByJava() {
        String json = """
            {"id": "abc"}
            """;
        noopWith("写法示例", noop -> noop
            .teardown(teardown -> teardown
                .hooks("${vars.put('x', 1)}")
                .extract(extract -> extract
                    .jsonpath("id", "$.id", params -> params.target(json)))
                .validate(validate -> validate
                    .equalTo("${x?int}", 1)
                    .equalTo("${id}", "abc"))));
    }

    @Test(description = "teardown/extract/validate 同级，Yaml 用例")
    public void test1_1ByYaml() {
        getSession().run("testcases/processor/teardown1_1.yml");
    }

    @Test(description = "extract/validate 位于 teardown 里面，Yaml 用例")
    public void test2_1ByYaml() {
        getSession().run("testcases/processor/teardown2_1.yml");
    }

    @Test(description = "extract/validate 位于 teardown 里面，Yaml 用例")
    public void test2_2ByYaml() {
        getSession().run("testcases/processor/teardown2_2.yml");
    }

    @Test(description = "extract/validate 位于 teardown 里面，Yaml 用例")
    public void test2_3ByYaml() {
        getSession().run("testcases/processor/teardown2_3.yml");
    }

    @Test(description = "type 异常测试：没有 type 字段", expectedExceptions = DataLoadException.class)
    public void test3_1ByYaml() {
        getSession().run("testcases/processor/teardown_err_3_1.yml");
    }

    @Test(description = "type 异常测试：type 的值不是 String 类型", expectedExceptions = DataLoadException.class)
    public void test3_2ByYaml() {
        getSession().run("testcases/processor/teardown_err_3_2.yml");
    }

    @Test(description = "type 异常测试：type 的值不是 extract 或 validate 或 default",
        expectedExceptions = DataLoadException.class)
    public void test3_3ByYaml() {
        getSession().run("testcases/processor/teardown_err_3_3.yml");
    }

}
