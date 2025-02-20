package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.support.Ref;
import com.liyunx.groot.testelement.TestCase;
import org.testng.annotations.Test;

import java.util.Map;

import static com.liyunx.groot.DefaultVirtualRunner.*;
import static com.liyunx.groot.SessionRunner.getSession;
import static org.assertj.core.api.Assertions.assertThat;

public class RefTestCaseControllerTest extends GrootTestNGTestCase {

    private final TestCase testCase = new TestCase.Builder()
        .name("执行一些复杂计算")
        .variables(variables -> variables
            .var("x", 0)        // input
            .var("y", 0)        // input
            .var("res", 0))     // output
        .steps(() -> {
            Ref<Integer> xValue = Ref.ref(v("x"));
            Ref<Integer> yValue = Ref.ref(v("y"));
            Ref<Integer> xTotal = Ref.ref(xValue.value);
            Ref<Integer> yTotal = Ref.ref(yValue.value);
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
        Ref<Boolean> isSuccess = Ref.ref(false);
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

}