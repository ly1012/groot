package com.liyunx.groot.processor;

import com.liyunx.groot.GrootTestNGTestCase;
import org.testng.annotations.Test;

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

}
