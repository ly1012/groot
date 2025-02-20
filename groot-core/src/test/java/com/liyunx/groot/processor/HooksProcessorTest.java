package com.liyunx.groot.processor;

import com.liyunx.groot.GrootTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;

public class HooksProcessorTest extends GrootTestNGTestCase {

    @Test
    public void testHooksProcessorByYaml() {
        getSession().run("testcases/processor/hooks.yml");
    }

}
