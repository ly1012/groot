package com.liyunx.groot.protocol.http.yaml;

import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.Test;

import java.io.File;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.SessionRunner.getSession;
import static org.assertj.core.api.Assertions.assertThat;

public class ResponseTest extends WireMockTestNGTestCase {

    @Test(description = "Response Body Save To File")
    public void testResponse_Download() {
        String projectDirectory = new File("").getAbsolutePath();
        if (!projectDirectory.endsWith("groot-http")) {
            throw new UnsupportedOperationException("项目路径获取失败，最后一个文件夹不是 groot-http");
        }
        sessionConfig(config -> config
            .variables(variables -> variables
                .var("projectDirectory", projectDirectory)));

        WireMock.stubFor(WireMock
            .get("/download")
            .willReturn(WireMock.aResponse()
                .withBodyFile("独孤九剑.txt")));

        getSession().run("testcases/response/download.yml");

        String rootDirectory = "src/test/resources/";
        assertThat(new File(rootDirectory + "download/独孤九剑孤本.txt"))
            .exists()
            .isFile()
            .canRead()
            .hasSameContentAs(new File(rootDirectory + "wiremock/__files/独孤九剑.txt"));
    }

}
