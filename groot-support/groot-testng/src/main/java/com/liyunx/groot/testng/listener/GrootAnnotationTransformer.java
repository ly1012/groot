package com.liyunx.groot.testng.listener;

import com.liyunx.groot.testng.annotation.DataSource;
import com.liyunx.groot.testng.dataprovider.DataSourceProvider;
import com.liyunx.groot.testng.support.AnnotationSupport;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Groot 注解转换
 */
public class GrootAnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (testMethod == null) {
            return;
        }

        DataSource dataSource = AnnotationSupport.getDataSource(testMethod);
        if (dataSource != null) {
            annotation.setDataProviderClass(DataSourceProvider.class);
            if (dataSource.parallel()) {
                annotation.setDataProvider(DataSourceProvider.DATA_SOURCE_PARALLEL);
            } else {
                annotation.setDataProvider(DataSourceProvider.DATA_SOURCE);
            }
        }
    }

}
