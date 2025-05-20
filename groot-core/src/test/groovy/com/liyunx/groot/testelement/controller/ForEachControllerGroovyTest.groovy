package com.liyunx.groot.testelement.controller

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.*
import static org.assertj.core.api.Assertions.assertThat

class ForEachControllerGroovyTest extends GrootTestNGTestCase {

    @Test
    void testForEachUsingDataWithExpression() {
        def data = [
            [name: "cat", comment: "HelloKitty"],
            [name: "dog", comment: "Snoopy"]
        ]
        sv("data", data)
        foreach("使用表达式返回的数据", { expression('${data}') }) {

            onIf("如果是猫", '${name == "cat"}') {
                assertThat((String) v("comment")).isEqualTo("HelloKitty")
            }

        }
    }

    @Test
    public void testForEachUsingFile() {
        int count
        foreach("使用 3 个不同权限的账号操作", "testdata/testelement/foreach/user.csv") {
            count++
            if (count == 3) {
                String username = lv("username")
                String password = lv("password")
                assertThat(username).isEqualTo("groot")
                assertThat(password).isEqualTo("grootPassword")
            }
        }
        assertThat(count).isEqualTo(3)
    }

    @Test
    public void testForEachUsingBuilderWithFilter() {
        int count
        def forSettings = {
            file "testcases/controller/foreach/data_filter.csv?ignoreSurroundingSpaces=true"
            filter {
                slice "[2..-1]"
                condition '${role == "guest" && username == "tom"}'
                names "role", "username", "password"
            }
        }
        foreach("ForEachController 过滤器示例", forSettings) {
            noopWith("变量值断言") {
                validate {
                    equalTo '${password}', "guest999"
                    equalTo '${comment}', null
                }
            }
            count++
        }
        assertThat(count).isEqualTo(1)
    }

}

