package com.liyunx.groot.builder

import com.liyunx.groot.GrootTestNGTestCase
import com.liyunx.groot.support.Ref
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.*
import static com.liyunx.groot.support.Ref.ref

class LazyBuilderGroovyTest extends GrootTestNGTestCase {

    Closure params =   {
        target '{"person":{"name":"jack","age":18}}'
    }

    @Test(description = "extract 和 validate 非 Lazy 模式执行")
    void testNonLazy() {
        Ref<String> personName = ref("")
        groupWith("非 Lazy 前后置示例") {
            extract {
                jsonpath 'personName', '$.person.name', this.params
                assert lv('personName') == "jack"

                jsonpath personName, '$.person.name', this.params
                assert personName.value == "jack"
            }
            validate {
                def expected = "jack"
                equalTo('${personName}', expected)
                equalTo(personName.value, expected)
            }
        }
    }

    @Test(description = "extract 和 validate Lazy 模式执行：先统一构建对象再执行")
    void testLazy() {
        Ref<String> personName = ref("")
        groupWith("Get 请求") {
            lazyExtract {
                jsonpath 'personName', '$.person.name', this.params
                assert lv('personName') == null   // 此时提取操作还未执行

                jsonpath personName, '$.person.name', this.params
                assert personName.value == ""           // 此时提取操作还未执行
            }
            lazyValidate {
                def expected = "jack"
                equalTo('${personName}', expected)
                equalTo(personName.value, "")   // 此时 lazyExtract 还未执行
            }
        }
    }

    @Test(description = "【推荐写法一】外部非 Lazy 模式，内部非 Lazy 模式")
    void testNonLazyNonLazy() {
        Ref<String> personName = ref("")
        sv("cnt", 0)
        groupWith("Get 请求") {
            println("执行序号：1")
            setupBefore {
                println("执行序号：3")
                hook('${vars.put("cnt", 1)}')     // 立即执行
                assert sv("cnt") == 1
            }
            // 注意！！！ 这句会先于 setupBefore 执行，通常不建议在 setupBefore/teardown 等方法的同级编写代码
            // setupBefore 是非 Lazy 模式，如果先执行 setupBefore 里面的代码会因为依赖的 ContextWrapper 对象还未创建而报错
            println("执行序号：2")
            // 外部是非 Lazy 模式，所以里面的后置处理器、extract、validate 方法会立即执行
            teardown {
                println("执行序号：4")
                hook('${print("执行序号：5")}')     // 立即执行
                // 外部是非 Lazy 模式，所以 extract 方法会立即执行
                // 同时 extract 是非 Lazy 模式，所以 extract 里面的方法也会立即执行
                extract {
                    println("执行序号：6")
                    jsonpath 'personName', '$.person.name', this.params   // 立即执行
                    assert lv('personName') == "jack"

                    jsonpath personName, '$.person.name', this.params
                    assert personName.value == "jack"
                }
                println("执行序号：7")
                validate {
                    println("执行序号：8")
                    def expected = "jack"
                    equalTo('${personName}', expected)    // 立即执行
                    equalTo(personName.value, expected)
                }
                extract {
                    println("执行序号：9")
                    jsonpath 'personAge', '$.person.age', this.params   // 立即执行
                    assert lv('personAge') == 18
                }
            }
        }
    }

    @Test(description = "【推荐写法二】外部和内部都是 Lazy 模式")
    void testLazyLazy() {
        Ref<String> personName = ref("")
        sv("cnt", 0)
        groupWith("Get 请求") {
            lazySetupBefore {
                hook('${vars.put("cnt", 1)}')      // 此时 Hook 前置还未执行
                assert sv("cnt") == 0
            }
            lazyTeardown {
                lazyExtract {
                    jsonpath 'personName', '$.person.name', this.params
                    assert lv('personName') == null   // 此时提取操作还未执行

                    jsonpath personName, '$.person.name', this.params
                    assert personName.value == ""           // 此时提取操作还未执行
                }
                lazyValidate {
                    def expected = "jack"
                    equalTo('${personName}', expected)
                    equalTo(personName.value, "")   // 此时 lazyExtract 还未执行
                }
            }
        }
    }

    @Test(description = "【不推荐】外部 Lazy 模式，内部非 Lazy 模式")
    void testLazyNonLazy() {
        Ref<String> personName = ref("")
        sv("cnt", 0)
        groupWith("Get 请求") {
            println("执行序号：1")
            lazySetupBefore {
                println("执行序号：2")
                hook('${vars.put("cnt", 1)}')      // 此时 Hook 前置还未执行
                assert sv("cnt") == 0
            }
            println("执行序号：3")
            // 外部是 Lazy 模式，所以里面的后置处理器、extract、validate 方法都是收集动作，而非执行动作
            // 统一收集完成后按顺序执行
            lazyTeardown {
                println("执行序号：4")
                hook('${print("执行序号：6")}')
                extract {
                    println("执行序号：7")
                    jsonpath 'personName', '$.person.name', this.params
                    assert lv('personName') == "jack"

                    jsonpath personName, '$.person.name', this.params
                    assert personName.value == "jack"
                }
                println("执行序号：5")
                validate {
                    println("执行序号：8")
                    def expected = "jack"
                    equalTo('${personName}', expected)
                    equalTo(personName.value, expected)
                }
            }
        }
    }

    @Test(description = "【不推荐】外部非 Lazy 模式，内部 Lazy 模式")
    void testNonLazyLazy() {
        Ref<String> personName = ref("")
        sv("cnt", 0)
        groupWith("Get 请求") {
            println("执行序号：1")
            setupBefore {
                println("执行序号：3")
                hook('${vars.put("cnt", 1)}')
                assert sv("cnt") == 1
            }
            println("执行序号：2")
            teardown {
                println("执行序号：4")
                hook('${print("执行序号：5")}')
                lazyExtract {
                    println("执行序号：6")
                    jsonpath 'personName', '$.person.name', this.params
                    assert lv('personName') == null

                    jsonpath personName, '$.person.name', this.params
                    assert personName.value == ""
                }
                println("执行序号：7")
                lazyValidate {
                    println("执行序号：8")
                    def expected = "jack"
                    equalTo('${personName}', expected)
                    equalTo(personName.value, expected)
                }
            }
        }
    }

}
