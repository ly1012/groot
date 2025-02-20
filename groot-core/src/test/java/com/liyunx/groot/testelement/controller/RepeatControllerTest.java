package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.Ref;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.*;
import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.support.Ref.ref;
import static org.assertj.core.api.Assertions.assertThat;

public class RepeatControllerTest extends GrootTestNGTestCase {

    @Test
    public void testRepeat() {
        Ref<Integer> count = ref(0);
        repeat("重复 3 次", 3, () -> {
            count.value++;
        });
        assertThat(count.value).isEqualTo(3);
    }

    @Test
    public void testRepeatByYaml() {
        getSession().run("testcases/controller/repeat/repeat.yml");
    }

    @Test
    public void testRepeatUsingExpression() {
        Ref<Integer> count = ref(0);
        repeat("重复 3 次", "${1 + 1 + 1}", () -> {
            count.value++;
        });
        assertThat(count.value).isEqualTo(3);
    }

    @Test
    public void testRepeatWith() {
        {
            // 写法一：配置和前后置构建放在 repeatWith 同级位置，适合构建代码较复杂的情况
            Ref<Integer> count = ref(0);
            Customizer<RepeatController.Builder> it = builder -> builder
                .times("3")
                .variables(variables -> variables
                    .var("x", "y"))
                .setupBefore(before -> before
                    .hook("${vars.put('x', 'z')}"))
                .validate(validate -> validate
                    .equalTo("${x}", "z"));
            repeatWith("重复 3 次", it, () -> {
                count.value++;
            });
            assertThat(count.value).isEqualTo(3);
        }

        {
            // 写法二：配置和前后置构建放在 repeatWith 参数位置，适合构建代码比较简单的情况
            Ref<Integer> count = ref(0);
            repeatWith("重复 3 次", builder -> builder
                .times("3")
                .variables(variables -> variables
                    .var("x", "y"))
                .setupBefore(before -> before
                    .hook("${vars.put('x', 'z')}"))
                .validate(validate -> validate
                    .equalTo("${x}", "z")), () ->
            {
                count.value++;
            });
            assertThat(count.value).isEqualTo(3);
        }

    }

}
