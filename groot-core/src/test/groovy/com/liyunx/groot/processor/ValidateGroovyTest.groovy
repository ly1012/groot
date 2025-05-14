package com.liyunx.groot.processor

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.noopWith

class ValidateGroovyTest extends GrootTestNGTestCase {

    @Test
    public void testGroovy_1() {
        noopWith("标准写法") {
            validate {
                equalTo "abc", "ABC", { ignoreCase() }
            }
        }
    }

}
