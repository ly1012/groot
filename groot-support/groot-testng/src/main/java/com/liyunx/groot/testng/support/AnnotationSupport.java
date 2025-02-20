package com.liyunx.groot.testng.support;

import com.liyunx.groot.testng.annotation.DataFilter;
import com.liyunx.groot.testng.annotation.DataSource;
import com.liyunx.groot.testng.annotation.GrootDataSource;
import com.liyunx.groot.util.AnnotationFormatException;
import com.liyunx.groot.util.AnnotationUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AnnotationSupport {

    public static DataSource getDataSource(Method method) {
        DataSource dataSource = method.getAnnotation(DataSource.class);
        if (dataSource != null) {
            return dataSource;
        }

        GrootDataSource grootDataSource = method.getAnnotation(GrootDataSource.class);
        if (grootDataSource == null) {
            return null;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("value", grootDataSource.value());
        values.put("parallel", grootDataSource.parallel());
        try {
            return AnnotationUtil.newInstance(DataSource.class, values);
        } catch (AnnotationFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataFilter getDataFilter(Method method) {
        DataFilter dataFilter = method.getAnnotation(DataFilter.class);
        if (dataFilter != null) {
            return dataFilter;
        }

        GrootDataSource grootDataSource = method.getAnnotation(GrootDataSource.class);
        if (grootDataSource == null) {
            return null;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("slice", grootDataSource.slice());
        values.put("expr", grootDataSource.expr());
        try {
            return AnnotationUtil.newInstance(DataFilter.class, values);
        } catch (AnnotationFormatException e) {
            throw new RuntimeException(e);
        }
    }

}
