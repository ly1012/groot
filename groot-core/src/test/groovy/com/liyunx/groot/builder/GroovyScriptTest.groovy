package com.liyunx.groot.builder

// 平台默认自动导入所有内置方法
import com.liyunx.groot.Groot
import com.liyunx.groot.SessionRunner
import com.liyunx.groot.support.Ref

import static com.liyunx.groot.DefaultVirtualRunner.*
import static com.liyunx.groot.support.Ref.ref

// 测试开始前准备
Groot groot = new Groot()
SessionRunner session = groot.newTestRunner().newSessionRunner()
SessionRunner.setSession(session)
session.start()

// 以下为测试用例代码，也是平台上需要编写的用例代码
// 测试用例代码前后的包装代码，可以封装进一个工具方法中，具体怎么封装取决于需求和设计
Closure params = {
    target '{"person":{"name":"jack","age":18}}'
}
Ref<String> personName = ref("")
sv("cnt", 0)
groupWith("Get 请求") {
    setupBefore {
        hook('${vars.put("cnt", 1)}')
        assert sv("cnt") == 1
    }
    teardown {
        hook('${2 + 3}')
        extract {
            jsonpath 'personName', '$.person.name', params
            assert lv('personName') == "jack"

            jsonpath personName, '$.person.name', params
            assert personName.value == "jack"
        }
        validate {
            def expected = "jack"
            equalTo('${personName}', expected)
            equalTo(personName.value, expected)
        }
        extract {
            jsonpath 'personAge', '$.person.age', params
            assert lv('personAge') == 18
        }
    }
}

pp()

private void pp() {
    println("pp")
}

// 测试结束后，清理资源
if (session != null) {
    session.stop();
    SessionRunner.removeSession();
}




