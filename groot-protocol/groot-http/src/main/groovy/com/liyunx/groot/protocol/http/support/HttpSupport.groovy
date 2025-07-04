package com.liyunx.groot.protocol.http.support

import com.liyunx.groot.protocol.http.annotation.DelegatesToHttpSamplerBuilder
import com.liyunx.groot.support.GroovySupport

class HttpSupport {

    static Closure[] mergeClosures(@DelegatesToHttpSamplerBuilder Closure beforeClosure,
        @DelegatesToHttpSamplerBuilder Closure[] closures,
        @DelegatesToHttpSamplerBuilder Closure afterClosure) {
        return GroovySupport.mergeClosures(beforeClosure, closures, afterClosure)
    }

}
