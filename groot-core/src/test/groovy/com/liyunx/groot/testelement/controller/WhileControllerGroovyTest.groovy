package com.liyunx.groot.testelement.controller

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.onWhile
import static com.liyunx.groot.DefaultVirtualRunner.sv
import static org.assertj.core.api.Assertions.assertThat

class WhileControllerGroovyTest extends GrootTestNGTestCase {

    @Test
    public void testOnWhile() {
        int maxTime = 200
        long start = System.currentTimeMillis()
        int count
        sv("total", 10)
        onWhile("条件表达式", '${total > 0}') {
            if (System.currentTimeMillis() - start > maxTime) {
                throw new RuntimeException("timeout")
            }
            sv("total", (Integer) sv("total") - 1)
            count++
        }
        assertThat(count).isEqualTo(10)
    }

    @Test
    public void testOnWhileWithLimit() {
        sv("cnt", 10)
        onWhile("超次限制", { condition '${cnt > 0}' limit 5 }) {
            sleep(10)
            sv("cnt", (Integer) sv("cnt") + 1)
        }
    }

    @Test
    public void testOnWhileWithTimeout() {
        sv("total", 10)
        onWhile("超时限制", { condition '${total > 0}' timeout(50) }) {
            sleep(10)
            sv("total", (Integer) sv("total") + 1)
        }
    }
}
