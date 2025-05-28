package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.support.Ref;
import com.liyunx.groot.testelement.TestCase;
import com.liyunx.groot.testelement.sampler.NoopSampler;
import org.testng.annotations.Test;

import java.util.Map;

import static com.liyunx.groot.DefaultVirtualRunner.*;
import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.support.Ref.ref;
import static org.assertj.core.api.Assertions.assertThat;

public class RefTestCaseControllerTest extends GrootTestNGTestCase {

    private final TestCase testCase = new TestCase.Builder()
        .name("执行一些复杂计算")
        .variables(variables -> variables
            .var("x", 0)        // input
            .var("y", 0)        // input
            .var("res", 0))     // output
        .steps(() -> {
            Ref<Integer> xValue = ref(v("x"));
            Ref<Integer> yValue = ref(v("y"));
            Ref<Integer> xTotal = ref(xValue.value);
            Ref<Integer> yTotal = ref(yValue.value);
            repeat("计算 x 的立方", 2, () -> {
                xTotal.value = xTotal.value * xValue.value;
            });
            repeat("计算 y 的平方", 1, () -> {
                yTotal.value = yTotal.value * yValue.value;
            });
            v("res", xTotal.value - yTotal.value);
        })
        .build();

    @Test
    public void testRefTestCase() {
        refTestCase("计算 5 的立方减去 3 的平方", testCase, Map.of(
            "x", 5, "y", 3, "res", 0
        )).then(r -> {
            assertThat(r.getVariables().get("res")).isEqualTo(5 * 5 * 5 - 3 * 3);
        });
    }

    @Test
    public void testRefTestCaseByYaml() {
        getSession().run("testcases/controller/refTestCase/refTestCase.yml");
    }

    @Test
    public void testRefTestCaseWith() {
        Ref<Boolean> isSuccess = ref(false);
        refTestCaseWith("计算 5 的立方减去 3 的平方", builder -> builder
            .variables(Map.of(
                "x", 5, "y", 3, "res", 0))
            .refTestCase(testCase)
            .validate(validate -> validate
                .apply(ctx -> {
                    assertThat((Integer) lv("res")).isEqualTo(5 * 5 * 5 - 3 * 3);
                    isSuccess.value = true;
                })));
        assertThat(isSuccess.value).isTrue();
    }

    @Test
    public void testRefTestCaseUsingObject() {
        Ref<Integer> outParam1 = ref();
        refTestCase("执行其他测试用例示例", bizLogicTestCase(), Map.of(
            "inParam1", 6, "inParam2", 6, "outParam1", 0
        )).then(r -> {
            // 提取用例执行结果
            outParam1.value = (Integer) r.getVariables().get("outParam1");
        });
        assertThat(outParam1.value).isEqualTo(30);
    }

    private TestCase bizLogicTestCase() {
        return new TestCase.Builder()
            .name("业务逻辑封装")
            .variables(variables -> variables
                .var("inParam1", 0)
                .var("inParam2", 0)
                .var("outParam1", 0))
            .step(new NoopSampler.Builder()
                .name("do something")
                .teardown(teardown -> teardown
                    .hooks("${vars.put('outParam1', (inParam1 * inParam1 - inParam2)?int)}")))
            .build();
    }

    @Test
    public void testRefTestCaseUsingMethod() {
        int res = bizLogic(6, 6);
        assertThat(res).isEqualTo(30);
    }

    private int bizLogic(int inParam1, int inParam2) {
        Ref<Integer> res = ref();
        noopWith("do something", action -> action
            .variables(variables -> variables
                .var("inParam1", inParam1)
                .var("inParam2", inParam2))
            .teardown(teardown -> teardown
                .hooks("${vars.put('outParam1', (inParam1 * inParam1 - inParam2)?int)}")
                .apply(ctx -> res.value = lv("outParam1"))));
        return res.value;
    }

}