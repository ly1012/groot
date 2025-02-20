package com.liyunx.groot.testng.annotation;

import com.liyunx.groot.testng.annotation.DataFilter;
import com.liyunx.groot.testng.annotation.DataSource;
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

}
