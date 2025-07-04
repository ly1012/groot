package com.liyunx.groot.protocol.http.response

import com.github.tomakehurst.wiremock.client.WireMock
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase
import groovy.transform.CompileStatic
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.httpWith
import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class ResponseGroovyTest extends WireMockTestNGTestCase {

    static class LoginResponse {
        String username
        String password
    }

    @Test
    void testBodyAs() {
        WireMock.stubFor(WireMock
            .post("/login")
            .willReturn(WireMock
                .okJson("""
                    {
                        "username": "jack",
                        "password": "123456"
                    }
                """)))

        httpWith('bodyAs method test') {
            request {
                post '/login'
                body 'G R O O T'
            }
            teardown {
                def loginResponse = r.response.bodyAs(LoginResponse.class)
                validate {
                    equalTo loginResponse.username,"jack"
                    equalTo loginResponse.password,"123456"
                }

            }
        }
    }

    @Test(description = "Response Body Save To File")
    void testResponse_Download() {
        String projectDirectory = new File("").getAbsolutePath()
        if (!projectDirectory.endsWith("groot-http")) {
            throw new UnsupportedOperationException("项目路径获取失败，最后一个文件夹不是 groot-http")
        }
        sessionConfig {
            variables {
                var "projectDirectory", projectDirectory
            }
        }

        WireMock.stubFor(WireMock
            .get("/download")
            .willReturn(WireMock.aResponse()
                .withBodyFile("独孤九剑.txt")))

        http("相对路径") {
            get "/download"
            download "download/独孤九剑孤本.txt"
        }
        assertDownloadFile()

        http("绝对路径") {
            get "/download"
            download '${projectDirectory}/src/test/resources/download/独孤九剑孤本.txt'
        }
        assertDownloadFile()
    }

    private void assertDownloadFile() {
        String rootDirectory = "src/test/resources/"
        File file = new File(rootDirectory + "download/独孤九剑孤本.txt")
        assertThat(file)
            .exists()
            .isFile()
            .canRead()
            .hasSameContentAs(new File(rootDirectory + "wiremock/__files/独孤九剑.txt"))
        file.delete()
    }

}
