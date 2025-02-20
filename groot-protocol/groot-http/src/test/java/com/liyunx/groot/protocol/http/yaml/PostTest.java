package com.liyunx.groot.protocol.http.yaml;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.liyunx.groot.protocol.http.constants.HttpHeader;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static com.liyunx.groot.SessionRunner.getSession;
import static java.util.UUID.randomUUID;

public class PostTest extends WireMockTestNGTestCase {

    private static final String MULTIPART_PATH = "testcases/post/multipart/";
    private static final String BINARY_PATH = "testcases/post/binary/";
    private static final String JSON_PATH = "testcases/post/json/";
    private static final String DATA_PATH = "testcases/post/data/";
    private static final String FORM_PATH = "testcases/post/form/";

    @Test(description = "MultiPart：上传单个文件")
    public void testMultiPart_SingleFile() {
        String url = "/multipart/upload";
        String contentDispositionValue1 = "form-data; name=\"file\"; filename=\"武功秘籍.txt\"";

        WireMock.stubFor(WireMock
            .post(url)
            // 默认匹配类型为 MultipartValuePattern.MatchingType.ANY
            // ANY：请求体中只要有一个 Part 匹配该 multiPartPattern 即成功
            // ALL：请求体中所有 Part 都匹配该 multiPartPattern 即成功
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue1)))
            .willReturn(WireMock
                .ok()));

        getSession().run(MULTIPART_PATH + "singleFile.yml");
    }

    @Test(description = "MultiPart：上传多个文件")
    public void testMultiPart_MultiFile() {
        String url = "/multipart/upload";
        String contentDispositionValue1 = "form-data; name=\"file\"; filename=\"降龙十八掌.txt\"";
        String contentDispositionValue2 = "form-data; name=\"file\"; filename=\"独孤九剑.txt\"";

        WireMock.stubFor(WireMock
            .post(url)
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue1)))
            .withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue2)))
            .willReturn(WireMock
                .ok()));

        getSession().run(MULTIPART_PATH + "multiFile.yml");
    }

    @Test(description = "MultiPart：Part Name 不重复")
    public void testMultiPart_UniqueName() {
        String url = "/multipart/unique";
        String[] contentDispositionValues = new String[]{
            "form-data; name=\"file\"; filename=\"降龙十八掌.txt\"",
            "form-data; name=\"helloMessage\"",
            "form-data; name=\"helloMsg\"",
            "form-data; name=\"orderDetail\""
        };

        MappingBuilder builder = WireMock.post(url);
        for (String contentDispositionValue : contentDispositionValues) {
            builder.withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue)));
        }
        builder.withMultipartRequestBody(WireMock.aMultipart()
            .withName("orderDetail")
            .withBody(WireMock.equalToJson("""
                {
                  "orderId": "123456789",
                  "owner": "groot",
                  "productList": [
                    {
                      "productId": "666",
                      "productName": "六六六"
                    },
                    {
                      "productId": "888",
                      "productName": "发发发"
                    }
                  ]
                }
                """)));
        WireMock.stubFor(builder.willReturn(WireMock.ok()));

        getSession().run(MULTIPART_PATH + "uniqueName.yml");
    }

    @Test(description = "MultiPart：Part Name 重复")
    public void testMultiPart_DuplicateName() {
        String url = "/multipart/duplicate";
        String[] contentDispositionValues = new String[]{
            "form-data; name=\"file\"; filename=\"中文.pdf\"",
            "form-data; name=\"k1\"",
            "form-data; name=\"k2\""
        };

        MappingBuilder builder = WireMock.post(url);
        for (String contentDispositionValue : contentDispositionValues) {
            builder.withMultipartRequestBody(WireMock.aMultipart()
                .withHeader("Content-Disposition", WireMock.equalTo(contentDispositionValue)));
        }
        builder.withMultipartRequestBody(WireMock.aMultipart()
            .withName("k1")
            .withBody(WireMock.equalToJson("""
                {
                    "mk1": "mv1",
                    "mk2": {
                        "mk3": "mv3"
                    }
                }
                """)));
        builder.withMultipartRequestBody(WireMock.aMultipart()
            .withName("k2")
            .withHeader("myHeader", WireMock.equalTo("myValue")));
        WireMock.stubFor(builder.willReturn(WireMock.ok()));

        getSession().run(MULTIPART_PATH + "duplicateName.yml");
    }

    @Test(description = "Binary: Base64 值表示 byte[](UTF-8 编码)")
    public void testBinary_Base64() {
        WireMock.stubFor(WireMock
            .post("/binary/base64")
            .withRequestBody(WireMock.binaryEqualTo("groot".getBytes(StandardCharsets.UTF_8)))
            .willReturn(WireMock
                .ok()));

        getSession().run(BINARY_PATH + "base64.yml");
    }

    @Test(description = "Binary: 请求 Body 为文件")
    public void testBinary_File() {
        WireMock.stubFor(WireMock
            .post("/binary/file")
            .withHeader(HttpHeader.CONTENT_TYPE.value(), WireMock.containing("application/pdf"))
            .willReturn(WireMock
                .ok()));

        getSession().run(BINARY_PATH + "file.yml");
    }

    @Test(description = "Binary: 表达式的值必须是 byte[] 或 File 类型")
    public void testBinary_Expression() {
        addFunction("randomByteArray_tpbe", (contextWrapper, parameters) -> {
            String value = randomUUID().toString();
            return value.getBytes(StandardCharsets.UTF_8);
        });

        WireMock.stubFor(WireMock
            .post("/binary/expression")
            .willReturn(WireMock.ok()));

        getSession().run(BINARY_PATH + "expression.yml");
    }

    @Test(description = "Json: 值类型为 String 或 Object")
    public void testJson_StringOrObject() {
        WireMock.stubFor(WireMock
            .post("/json/stringOrObject")
            .withHeader(HttpHeader.CONTENT_TYPE.value(), WireMock.containing("application/json"))
            .withRequestBody(WireMock.equalToJson("""
                {
                  "name": "groot",
                  "age": 18
                }"""))
            .willReturn(WireMock
                .ok()));

        getSession().run(JSON_PATH + "stringOrObject.yml");
    }

    @Test(description = "Json: 表达式返回 JSON 字符串或返回对象")
    public void testJson_Expression() {
        addFunction("returnJsonString_tpje", (ctx, params) -> """
            {
              "name": "groot",
              "age": 18
            }
            """);
        addFunction("returnObject_tpje", (ctx, params) -> new HashMap<String, Object>() {{
            put("name", "groot");
            put("age", 18);
        }});

        WireMock.stubFor(WireMock
            .post("/json/expression")
            .withRequestBody(WireMock.equalToJson("""
                {
                  "name": "groot",
                  "age": 18
                }
                """))
            .willReturn(WireMock.ok()));

        getSession().run(JSON_PATH + "expression.yml");
    }

    @Test(description = "Data: 值类型为 String 或 Object")
    public void testData_StringOrObject() {
        WireMock.stubFor(WireMock
            .post("/data/stringOrObject")
            .withHeader(HttpHeader.CONTENT_TYPE.value(), WireMock.containing("application/json"))
            .withRequestBody(WireMock.equalToJson("""
                {
                  "name": "groot",
                  "age": 18
                }
                """))
            .willReturn(WireMock
                .ok()));

        getSession().run(DATA_PATH + "stringOrObject.yml");
    }

    @Test(description = "Data: 表达式返回 JSON 字符串或返回对象")
    public void testData_Expression() {
        addFunction("returnJsonString_tpde", (ctx, params) -> """
            {
              "name": "groot",
              "age": 18
            }
            """);
        addFunction("returnObject_tpde", (ctx, params) -> new HashMap<String, Object>() {{
            put("name", "groot");
            put("age", 18);
        }});

        WireMock.stubFor(WireMock
            .post("/data/expression")
            .withRequestBody(WireMock.equalToJson("""
                {
                  "name": "groot",
                  "age": 18
                }
                """))
            .willReturn(WireMock.ok()));

        getSession().run(DATA_PATH + "expression.yml");
    }

    @Test(description = "Form(application/x-www-form-urlencoded): Key 不重复")
    public void testForm_UniqueName() {
        String requestBody = "hello=" + URLEncoder.encode("您好", StandardCharsets.UTF_8) + "&id=1";

        WireMock.stubFor(WireMock
            .post("/form/unique")
            .withHeader(HttpHeader.CONTENT_TYPE.value(), WireMock.containing("application/x-www-form-urlencoded"))
            .withRequestBody(WireMock.equalTo(requestBody))
            .willReturn(WireMock
                .ok()));

        getSession().run(FORM_PATH + "uniqueName.yml");
    }

    @Test(description = "Form(application/x-www-form-urlencoded): Key 重复")
    public void testForm_DuplicateName() {
        String requestBody = "hello=" + URLEncoder.encode("您好", StandardCharsets.UTF_8) + "&id=1&id=2";

        WireMock.stubFor(WireMock
            .post("/form/duplicate")
            .withHeader(HttpHeader.CONTENT_TYPE.value(), WireMock.containing("application/x-www-form-urlencoded"))
            .withRequestBody(WireMock.equalTo(requestBody))
            .willReturn(WireMock
                .ok()));

        getSession().run(FORM_PATH + "duplicateName.yml");
    }


}
