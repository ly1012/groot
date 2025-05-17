package com.liyunx.groot.protocol.http.filter

import com.liyunx.groot.annotation.KeyWord
import com.liyunx.groot.context.ContextWrapper
import com.liyunx.groot.filter.SampleFilterChain
import com.liyunx.groot.protocol.http.HttpRealResponse

@KeyWord("bankServiceEncryptDecrypt2")
class BankServiceEncryptDecryptFilter2 extends BankServiceEncryptDecryptFilter {

    private String key = "1234567890"

    BankServiceEncryptDecryptFilter2() {
    }

    BankServiceEncryptDecryptFilter2(String key) {
        this.key = key
    }

    @Override
    public void doSample(ContextWrapper ctx, SampleFilterChain chain) {
        super.doSample(ctx, chain)
        HttpRealResponse realResponse = ctx.testResult.response as HttpRealResponse
        realResponse.headers.setHeader("encryptKey", key)
    }

    String getKey() {
        return key
    }

    void setKey(String key) {
        this.key = key
    }
}
