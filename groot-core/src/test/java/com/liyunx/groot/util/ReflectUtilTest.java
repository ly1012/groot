package com.liyunx.groot.util;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.constants.GrootConstants;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.testelement.controller.IfController;
import com.liyunx.groot.testelement.controller.RefTestCaseController;
import com.liyunx.groot.util.ReflectUtil;
import org.testng.annotations.Test;

import java.util.Set;

import static com.liyunx.groot.constants.GrootConstants.GROOT_BASE_PACKAGE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ReflectUtilTest {

    @Test(description = "查找指定包下指定类型的所有实现类")
    public void testScanSubImplTypes() {
        Set<Class<? extends TestElement>> classes = ReflectUtil.scanImplTypes(GROOT_BASE_PACKAGE_NAME, TestElement.class);
        // 至少包含内置的 TestElement 实现类
        assertThat(classes).contains(IfController.class).contains(RefTestCaseController.class);
    }

    @Test(description = "查找包含指定注解的所有类")
    public void testScanImplTypesByAnnotation() {
        Set<Class<?>> classes = ReflectUtil.scanImplTypesByAnnotation(GROOT_BASE_PACKAGE_NAME, KeyWord.class);
        // 所有查找到的类都应该有 KeyWord 注解
        classes.forEach(clazz -> assertThat(clazz.getAnnotation(KeyWord.class)).isNotNull());
        // 至少包含内置的 TestElement 实现类
        assertThat(classes).contains(IfController.class).contains(RefTestCaseController.class);
    }

}
