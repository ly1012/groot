package com.liyunx.groot.testelement.controller

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.repeat
import static org.assertj.core.api.Assertions.assertThat

class RepeatControllerGroovyTest extends GrootTestNGTestCase {

    @Test
    void testRepeat() {
        int count
        repeat("重复常量次", 3) {
            count++;
        }
        assertThat(count).isEqualTo(3);

        int count2
        repeat("重复次数使用表达式计算", '${1 + 1 + 1}') {
            count2++;
        }
        assertThat(count2).isEqualTo(3);
    }

}
