package com.liyunx.groot.builder

import com.liyunx.groot.ApplicationConfig
import com.liyunx.groot.Groot

// 平台默认自动导入所有内置方法

import com.liyunx.groot.SessionRunner
import com.liyunx.groot.processor.extractor.standard.JsonPathExtractor
import com.liyunx.groot.support.Ref

import java.nio.file.Paths

import static com.liyunx.groot.DefaultVirtualRunner.*
import static com.liyunx.groot.support.GroovySupport.defClosure
import static com.liyunx.groot.support.Ref.ref

// IDEA 右键直接运行时：
// 记得修改 Run/Debug Configurations 的 Working Directory 为模块根目录，即 $ModuleFileDir$
// IDEA 默认将 groovy 脚本文件所在目录当做 JVM 工作目录（执行命令时的终端路径）

// 有兴趣的朋友可以写个简单的 Groot IDEA 插件，右键运行时根据插件配置自动完成工作目录设置、前后置处理等操作
// 然后 groovy 脚本可以只编写测试用例代码
// Yaml 脚本也类似，可以通过插件来实现右键运行（或者插件 + CLI），不依赖 TestNG 类

// 测试开始前准备
println Paths.get(ApplicationConfig.getWorkDirectory()).toAbsolutePath()
Groot groot = new Groot()
SessionRunner session = groot.newTestRunner().newSessionRunner()
SessionRunner.setSession(session)
session.start()

// 以下为测试用例代码，也是平台上需要编写的用例代码
// 测试用例代码前后的包装代码，可以封装进一个工具方法中，具体怎么封装取决于需求和设计
def params = defClosure(JsonPathExtractor.Builder.class) {
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




