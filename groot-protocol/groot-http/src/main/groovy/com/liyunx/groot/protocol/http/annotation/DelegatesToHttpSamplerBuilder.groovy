package com.liyunx.groot.protocol.http.annotation

import com.liyunx.groot.protocol.http.HttpSampler.Builder
import groovy.transform.AnnotationCollector

import java.lang.annotation.ElementType

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
// 显示导入 Builder 内部类，如果是 HttpSampler.Builder.class，
// 1. IntelliJ IDEA 绿色箭头运行单个用例时，报如下的错误，Groovy 编译器将 Builder 识别成了属性，而非内部类（具体原因未知）
// Groovyc: [Static type checking] - No such property: Builder for class: com.liyunx.groot.protocol.http.HttpSampler
// 2. mvn clean test 运行正常（IDEA 直接运行单个用例编译失败，不知道是不是 IDEA 的 Groovy 设置问题？）
@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = Builder.class)
@AnnotationCollector
@interface DelegatesToHttpSamplerBuilder {

}
