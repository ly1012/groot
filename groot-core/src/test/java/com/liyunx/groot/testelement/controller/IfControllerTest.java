package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.*;
import static com.liyunx.groot.SessionRunner.getSession;
import static org.assertj.core.api.Assertions.assertThat;

public class IfControllerTest extends GrootTestNGTestCase {

    @Test
    public void testOnIf() {
        sv("total", 10);
        onIf("大于 5 时执行", "${total > 5}", () -> {
            sv("res1", "success");
        });
        onIf("小于 5 时执行", "${total < 5}", () -> {
            sv("res2", "success");
        });
        assertThat((String) sv("res1")).isEqualTo("success");
        assertThat((String) sv("res2")).isNull();
    }

    @Test
    public void testOnIf2() {
        foreach("多个账号数据", "testcases/controller/if/data.csv", () -> {
            onIf("登录后台管理系统", "${role == 'admin'}", () -> {
                System.out.println(evalAsString("管理员 ${username} 登录后台"));
            });
            onIf("登录前台页面", "${role == 'guest'}", () -> {
                System.out.println(evalAsString("用户 ${username} 登录前台"));
            });
        });
    }

    @Test
    public void testOnIfByYaml() {
        getSession().run("testcases/controller/if/if.yml");
    }

    @Test
    public void testOnIfWith() {
        sv("total", 10);
        onIfWith("大于 5 时执行", it -> it
            .variables(variables -> variables.var("cnt", 5))
            .condition("${total > cnt}"), () ->
        {
            sv("res1", "success");
        });
        onIfWith("小于 5 时执行", it -> it
            .variables(variables -> variables.var("cnt", 5))
            .condition("${total < cnt}"), () ->
        {
            sv("res2", "success");
        });
        assertThat((String) sv("res1")).isEqualTo("success");
        assertThat((String) sv("res2")).isNull();
    }

}
