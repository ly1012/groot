package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.support.Ref;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.onWhile;
import static com.liyunx.groot.DefaultVirtualRunner.sv;
import static com.liyunx.groot.SessionRunner.getSession;
import static org.assertj.core.api.Assertions.assertThat;

public class WhileControllerTest extends GrootTestNGTestCase {

    @Test
    public void testOnWhile() {
        sv("total", 10);
        int maxTime = 200;
        long start = System.currentTimeMillis();
        Ref<Integer> count = Ref.ref(0);
        // 这里对变量的使用还是比较繁琐，后续考虑 condition 支持 lambda 表达式，配合 Ref 类进行简化，下面是一个初步设想：
        // 如果是 Groovy 用例，可以进一步简化，因为 Groovy 默认会将变量包装为引用类型
        // Ref<Integer> total = ref(10);    // 替换 sv("total", 10);
        // onWhile("while", () -> total.value > 0, () -> {
        //     total.value--;               // 替换 sv("total", (Integer) sv("total") - 1);
        // })
        onWhile("条件表达式", "${total > 0}", () -> {
            if (System.currentTimeMillis() - start > maxTime) {
                throw new RuntimeException("timeout");
            }
            sv("total", (Integer) sv("total") - 1);
            count.value++;
        });
        assertThat(count.value).isEqualTo(10);
    }

    @Test
    public void testOnWhileWithConditionByYaml() {
        getSession().run("testcases/controller/while/while_condition.yml");
    }

    @Test
    public void testOnWhileWithLimitByYaml() {
        getSession().run("testcases/controller/while/while_limit.yml");
    }

    @Test
    public void testOnWhileWithTimeoutByYaml() {
        getSession().run("testcases/controller/while/while_timeout.yml");
    }

    @Test
    public void testOnWhileWithUsingBuilderWithTimeout() {
        sv("total", 10);
        Ref<Integer> count = Ref.ref(0);
        long start = System.currentTimeMillis();
        onWhile("超时限制", it -> it.condition("${total > 0}").timeout(50), () -> {
            if (System.currentTimeMillis() - start > 200) {
                throw new RuntimeException("timeout");
            }
            sleep(10);
            sv("total", (Integer) sv("total") + 1);
            count.value++;
        });
        assertThat(count.value).isLessThan(6);
    }

    @Test
    public void testOnWhileWithUsingBuilderWithLimit() {
        sv("total", 10);
        Ref<Integer> count = Ref.ref(0);
        long start = System.currentTimeMillis();
        onWhile("超次限制", it -> it.condition("${total > 0}").limit(10), () ->
        {
            if (System.currentTimeMillis() - start > 200) {
                throw new RuntimeException("timeout");
            }
            sv("total", (Integer) sv("total") + 1);
            count.value++;
        });
        assertThat(count.value).isLessThan(11);
    }

    private void sleep(long timeInMillis) {
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
