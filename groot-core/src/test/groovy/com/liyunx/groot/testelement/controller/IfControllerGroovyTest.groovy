package com.liyunx.groot.testelement.controller

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.*

class IfControllerGroovyTest extends GrootTestNGTestCase {

    @Test
    public void testOnIf() {
        foreach("多个账号数据", "testcases/controller/if/data.csv") {
            onIf("登录后台管理系统", '${role == "admin"}') {
                System.out.println(evalAsString('管理员 ${username} 登录后台'));
            }
            onIf("登录前台页面", '${role == "guest"}') {
                System.out.println(evalAsString('用户 ${username} 登录前台'));
            }
        }
    }

}
