package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.support.Ref;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.*;
import static com.liyunx.groot.SessionRunner.getSession;
import static org.assertj.core.api.Assertions.assertThat;

public class GroupControllerTest extends GrootTestNGTestCase {

    @Test
    public void testGroup() {
        Ref<Integer> count = Ref.ref(0);
        group("先这样做", () -> {
            count.value++;
        });
        group("再那样做", () -> {
            count.value--;
        });
        assertThat(count.value).isEqualTo(0);
    }

    @Test
    public void testGroupByYaml() {
        getSession().run("testcases/controller/group/group.yml");
    }

    @Test
    public void testGroupWith() {
        groupWith("执行一系列动作", it -> it
            .variables(variables -> variables
                .var("initValue", "+")), () -> {

            String initValue = lv("initValue");
            assertThat(initValue).isEqualTo("+");
            sv("initValue", initValue.repeat(6));
        });
        assertThat((String) sv("initValue")).isEqualTo("++++++");
    }

    @Test
    public void testGroupExample() {
        group("用户 A 发送邮件", () -> {
            System.out.println("用户 A 登录");
            System.out.println("发送邮件 to B");
        });
        group("用户 B 接收邮件", () -> {
            System.out.println("用户 B 登录");
            System.out.println("用户 B 查看邮件");
        });
    }
}