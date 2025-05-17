package com.liyunx.groot.protocol.http.filter

import com.liyunx.groot.annotation.KeyWord
import com.liyunx.groot.common.Unique
import com.liyunx.groot.context.ContextWrapper
import com.liyunx.groot.filter.SampleFilterChain
import com.liyunx.groot.filter.TestFilter
import com.liyunx.groot.protocol.http.HttpSampleResult
import com.liyunx.groot.protocol.http.HttpSampler
import com.liyunx.groot.protocol.http.model.HttpRequest

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import static com.liyunx.groot.protocol.http.model.HttpRequest.BodyType.*

@KeyWord("bankServiceEncryptDecrypt")
class BankServiceEncryptDecryptFilter implements TestFilter, Unique {

    @Override
    String uniqueId() {
        return "bankServiceEncryptDecrypt"
    }

    @Override
    public boolean match(ContextWrapper ctx) {
        return ctx.getTestElement() instanceof HttpSampler
    }

    @Override
    public void doSample(ContextWrapper ctx, SampleFilterChain chain) {
        // 获取运行时数据
        HttpRequest request = ((HttpSampler) ctx.testElement).running.request
        request.bodyAutoComplete(ctx) // 自动补全，转为最终要发送的数据，类型为 byte[]/File/String
        // 请求加密（这里忽略了 form / multipart 类型请求 Body）
        byte[] originRequestBody
        switch (request.bodyType) {
            case JSON:    // request.json 类型为 String
                originRequestBody = ((String)request.json).getBytes(StandardCharsets.UTF_8)
                request.json = Base64.getEncoder().encodeToString(originRequestBody)
                break
            case BINARY:  // request.binary 类型为 byte[]/File
                if (request.binary instanceof File) {
                    File file = request.binary as File
                    originRequestBody = Files.readAllBytes(file.toPath())
                } else {
                    originRequestBody = request.binary as byte[]
                }
                request.binary = Base64.getEncoder().encodeToString(originRequestBody)
                break
            case DATA:    // request.binary 类型为 byte[]/File/String
                if (request.data instanceof File) {
                    File file = request.data as File
                    originRequestBody = Files.readAllBytes(file.toPath())
                } else if (request.data instanceof String) {
                    String requestData = request.data
                    originRequestBody = requestData.getBytes(StandardCharsets.UTF_8)
                } else {
                    originRequestBody = request.data as byte[]
                }
                request.data = Base64.getEncoder().encodeToString(originRequestBody)
                break
        }
        // 发起请求
        chain.doSample(ctx)
        // 响应解密
        HttpSampleResult result = (HttpSampleResult) ctx.testResult
        byte[] decodedResponseBodyBytes = Base64.getDecoder().decode(result.response.body.getBytes(StandardCharsets.UTF_8))
        result.response.body = new String(decodedResponseBodyBytes, StandardCharsets.UTF_8);
    }

}
