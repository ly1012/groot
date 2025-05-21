package com.liyunx.groot.testelement.controller

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.group

class GroupControllerGroovyTest extends GrootTestNGTestCase {

    @Test
    public void testGroupExample() {
        group("用户 A 发送邮件") {
            println "用户 A 登录"
            println "发送邮件 to B"
        }
        group("用户 B 接收邮件") {
            println "用户 B 登录"
            println "用户 B 查看邮件"
        }
    }
}
