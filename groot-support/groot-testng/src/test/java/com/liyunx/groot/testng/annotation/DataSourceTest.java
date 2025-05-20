package com.liyunx.groot.testng.annotation;

import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DataSourceTest {

    @DataSource("data/test.json")
    @DataFilter(slice = "[-1]")
    @Test
    public void testAnnotationTransformer(Map<String, Object> data) {
        assertThat(data.get("username")).isEqualTo("user");
        assertThat(data.get("password")).isEqualTo("666666");
    }

    @DataSource("data/test.json")
    @Test
    public void testAnnotationTransformer2(Map<String, Object> data) {
        System.out.println(data.get("username") + ":" + data.get("password"));
    }

}
