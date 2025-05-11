package com.liyunx.groot.processor;

import com.liyunx.groot.GrootTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.noopWith;
import static com.liyunx.groot.DefaultVirtualRunner.sv;
import static com.liyunx.groot.SessionRunner.getSession;

public class HooksProcessorTest extends GrootTestNGTestCase {

    @Test
    public void testHooksProcessorByYaml() {
        getSession().run("testcases/processor/hooks.yml");
    }

    @Test
    public void testHooksProcessorByYaml1() {
        getSession().run("testcases/processor/hooks1.yml");
    }

    @Test
    public void testHooksProcessorByYaml1_1() {
        getSession().run("testcases/processor/hooks1_1.yml");
    }

    @Test
    public void testHooksProcessorByYaml2() {
        getSession().run("testcases/processor/hooks2.yml");
    }

    @Test
    public void testHooksProcessorByYaml2_1() {
        getSession().run("testcases/processor/hooks2_1.yml");
    }

    @Test
    public void testHooksProcessorByYaml3() {
        getSession().run("testcases/processor/hooks3.yml");
    }

    @Test
    public void testHooksProcessorByJava() {
        sv("x", 0);
        sv("y", 0);
        noopWith("前置处理器标准写法测试", noop -> noop
            .setupBefore(setup -> setup
                .hooks(hooks -> hooks
                    .hook("${vars.put('x', x + 1)}")
                    .hook("${vars.put('y', y + 1)}")))
            .setupAfter(setup -> setup
                .hooks(hooks -> hooks
                    .hook("${vars.put('x', x + 1)}")
                    .hook("${vars.put('y', y + 1)}")))
            .validate(validate -> validate
                .equalTo("${(x + y)?int}", 4)));
    }

    @Test
    public void testHooksProcessorByJava2() {
        sv("x", 0);
        sv("y", 0);
        noopWith("前置处理器合并写法测试", noop -> noop
            .setup(setup -> setup
                .before(before -> before
                    .hooks(hooks -> hooks
                        .hook("${vars.put('x', x + 1)}")
                        .hook("${vars.put('y', y + 1)}")))
                .after(after -> after
                    .hooks(hooks -> hooks
                        .hook("${vars.put('x', x + 1)}")
                        .hook("${vars.put('y', y + 1)}"))))
            .validate(validate -> validate
                .equalTo("${(x + y)?int}", 4)));
    }

}
