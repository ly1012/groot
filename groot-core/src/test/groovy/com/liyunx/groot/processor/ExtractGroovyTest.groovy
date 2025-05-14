package com.liyunx.groot.processor


import com.liyunx.groot.GrootTestNGTestCase
import com.liyunx.groot.processor.extractor.ExtractScope
import com.liyunx.groot.support.Ref
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.noopWith
import static com.liyunx.groot.DefaultVirtualRunner.sv
import static com.liyunx.groot.support.Ref.ref

class ExtractGroovyTest extends GrootTestNGTestCase {

    @Test
    void testGroovy_1() {
        noopWith("refName 写法") {
            extract {
                jsonpath 'id', '$.id', { target '{"id": "abc"}' }
            }
            validate {
                equalTo '${id}', 'abc'
            }
        }
    }

    @Test
    void testGroovy_1_1() {
        noopWith("refName 写法") {
            extract {
                jsonpath 'id', '$.id', { target '{"id": "abc"}' scope ExtractScope.SESSION }
            }
            validate {
                equalTo '${id}', 'abc'
            }
        }
        assert sv("id") == "abc"
    }

    @Test
    void testGroovy_2() {
        Ref<String> id = ref("")
        noopWith("ref 写法") {
            extract {
                jsonpath id, '$.id', { target '{"id": "abc"}' }
            }
            validate {
                equalTo id.value, 'abc'
            }
        }
        assert Objects.equals(id.value, "abc")
    }

}
