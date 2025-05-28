package com.liyunx.groot.processor

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.noopWith

class TeardownGroovyTest extends GrootTestNGTestCase {

    @Test(description = "teardown/extract/validate 同级")
    void test1() {
        String json = """
            {"id": "abc"}
            """;
        noopWith("同级写法") {
            teardown {
                hooks '${vars.put("x", 1)}'
            }
            extract {
                jsonpath "id", '$.id', { target json }
            }
            validate {
                equalTo '${x?int}', 1
                equalTo '${id}', "abc"
            }
        }
    }

    @Test(description = "extract 和 validate 位于 teardown 中")
    void test2() {
        String json = """
            {"id": "abc"}
            """;
        noopWith("写法示例") {
            teardown {
                hooks '${vars.put("x", 1)}'
                extract {
                    jsonpath "id", '$.id', { target json }
                }
                validate {
                    equalTo '${x?int}', 1
                    equalTo '${id}', "abc"
                }
            }
        }
    }

}
