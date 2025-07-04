package com.liyunx.groot.protocol.http.annotation

import com.liyunx.groot.protocol.http.model.HttpRequest.Builder
import groovy.transform.AnnotationCollector

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = Builder.class)
@AnnotationCollector
@interface DelegatesToHttpRequestBuilder {

}
