package com.liyunx.groot.protocol.http.dataloader

import com.alibaba.fastjson2.JSON
import com.liyunx.groot.protocol.http.HttpSampler
import com.liyunx.groot.protocol.http.model.Header
import com.liyunx.groot.protocol.http.model.HeaderManager
import com.liyunx.groot.protocol.http.model.MultiPart
import com.liyunx.groot.protocol.http.model.Part
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

/**
 * FastJson2 不支持场景测试，当前在字段上指定自定义反序列化类来支持（见 {@link MultiPart} 行注释）。
 */
class FastJson2NotSupportTest {

    // 默认支持：非嵌套 ArrayList 子类的场景
    @Test(description = "ArrayList 子类 Json 反序列化测试")
    void testHeaderManager() {
        String raw = '''
        [{
            "name": "Content-Disposition",
            "value": "form-data; name=\\"password\\""
          }, {
            "name": "Content-Type",
            "value": "text/plain"
          }]
        '''
        HeaderManager headers = JSON.parseObject(raw, HeaderManager.class)
        assertThat(headers[0].class).isEqualTo(Header.class)
    }

    // 默认不支持：嵌套 ArrayList 子类的场景，如 MultiPart -> Part -> HeaderManager -> JSONObject
    // 当前在字段上指定自定义反序列化类，以下为测试代码
    @Test(description = "MultiPart，嵌套 ArrayList 子类 Json 反序列化测试")
    void testMultiPart() {
        String raw = '''
        [{
          "headers": [{
            "name": "Content-Disposition",
            "value": "form-data; name=\\"password\\""
          }, {
            "name": "Content-Type",
            "value": "text/plain"
          }],
          "name": "password",
          "body": "@#x8723"
        }, {
          "file": "simple_file.txt",
          "name": "file"
        }]
        '''
        MultiPart multiPart = JSON.parseObject(raw, MultiPart.class)
        assertThat(multiPart[0].headers[0].class).isEqualTo(Header.class)
    }

    // 默认不支持，HttpSampler -> HttpRequest -> MultiPart -> JSONObject
    // 同 testMultiPart，以下为测试代码
    @Test(description = "HttpSampler，嵌套 ArrayList 子类 Json 反序列化测试")
    void testHttpSampler() {
        String raw = '''
        {
          "name": "multipart 上传文件",
          "http": {
            "url": "https://httpbin.org/post"
            "multipart": [{
              "headers": [{
                "name": "Content-Disposition",
                "value": "form-data; name=\\"password\\""
              }, {
                "name": "Content-Type",
                "value": "text/plain"
              }],
              "name": "password",
              "body": "@#x8723"
            }, {
              "file": "simple_file.txt",
              "name": "file"
            }],
          }
        }
        '''
        HttpSampler sampler = JSON.parseObject(raw, HttpSampler.class)
        assertThat(sampler.request.multipart[0].class).isEqualTo(Part.class)
        assertThat(sampler.request.multipart[0].headers[0].class).isEqualTo(Header.class)
    }

}
