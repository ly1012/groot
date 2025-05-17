package com.liyunx.groot.support;

import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.controller.ForEachController;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("rawtypes")
public class GrootServiceLoaderTest {

    @Test
    public void testLoadAsListBySPI() {
        List<TestElement> list = GrootServiceLoader.loadAsListBySPI(TestElement.class);
        assertThat(list).hasAtLeastOneElementOfType(ForEachController.class);
    }

    @Test
    public void testLoadAsListByScanPackage() {
        List<TestElement> list = GrootServiceLoader.loadAsListByScanPackage(TestElement.class);
        assertThat(list).hasAtLeastOneElementOfType(ForEachController.class);
    }

    @Test
    public void testLoadAsMapBySPI() {
        Map<String, Class<? extends TestElement>> map = GrootServiceLoader.loadAsMapBySPI(TestElement.class);
        assertThat(map).containsKey("for");
    }

    @Test
    public void testLoadAsMapByScanPackage() {
        Map<String, Class<? extends TestElement>> map = GrootServiceLoader.loadAsMapByScanPackage(TestElement.class);
        assertThat(map).containsKey("for");
    }

}