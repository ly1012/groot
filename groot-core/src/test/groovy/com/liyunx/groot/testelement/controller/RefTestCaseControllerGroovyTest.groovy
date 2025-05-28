package com.liyunx.groot.testelement.controller

import com.liyunx.groot.GrootTestNGTestCase
import com.liyunx.groot.testelement.TestCase
import com.liyunx.groot.testelement.sampler.NoopSampler
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.*
import static com.liyunx.groot.support.GroovySupport.defBuilder
import static org.assertj.core.api.Assertions.assertThat

class RefTestCaseControllerGroovyTest extends GrootTestNGTestCase {

    @Test
    public void testRefTestCaseUsingObject() {
        int outParam1 = 0
        refTestCase("执行其他测试用例示例", bizLogicTestCase(),
            ["inParam1": 6, "inParam2": 6, "outParam1": 0]
        ).then {
            // 提取用例执行结果
            outParam1 = it.getVariables().get("outParam1") as int
        }
        assertThat(outParam1).isEqualTo(30)
    }

    private TestCase bizLogicTestCase() {
        return defBuilder(TestCase.Builder.class) {
            name "业务逻辑封装"
            variables {
                var "inParam1", 0
                var "inParam2", 0
                var "outParam1", 0
            }
            step(defBuilder(NoopSampler.Builder.class) {
                name("do something")
                teardown {
                    hooks '${vars.put("outParam1", (inParam1 * inParam1 - inParam2) ? int)}'
                }
            })
        }.build()
    }

    @Test
    public void testRefTestCaseUsingMethod() {
        int res = bizLogic(6, 6)
        assertThat(res).isEqualTo(30)
    }

    private int bizLogic(int inParam1, int inParam2) {
        int res = 0
        noopWith("do something") {
            variables {
                var "inParam1", inParam1
                var "inParam2", inParam2
            }
            teardown {
                hooks '${vars.put("outParam1", (inParam1 * inParam1 - inParam2) ? int)}'
                apply {
                    res = lv("outParam1")
                }
            }
        }
        return res;
    }

}
