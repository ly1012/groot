package com.liyunx.groot.protocol.http.request;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.filter.RunFilterChain;
import com.liyunx.groot.filter.TestFilter;
import com.liyunx.groot.processor.assertion.Assertion;
import com.liyunx.groot.protocol.http.HttpSampler;
import com.liyunx.groot.protocol.http.WireMockTestNGTestCase;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.liyunx.groot.DefaultVirtualRunner.sessionConfig;
import static com.liyunx.groot.protocol.http.HttpVirtualRunner.http;

public class MethodTest extends WireMockTestNGTestCase {

    @Test
    public void testMethod() {
        String[] methodValues = new String[]{"get", "post", "put", "delete", "patch", "head", "options", "trace", "connect"};
        for (String method : methodValues) {
            registerStub(method.toUpperCase());
        }

        sessionConfig(config -> config
            .filters(new TestFilter() {
                @Override
                public boolean match(ContextWrapper ctx) {
                    return ctx.getTestElement() instanceof HttpSampler;
                }

                @Override
                public void doRun(ContextWrapper ctx, RunFilterChain chain) {
                    HttpSampler sampler = (HttpSampler) ctx.getTestElement();
                    List<Assertion> assertions = sampler.getRunning().getAssert_();
                    assertions = assertions == null ? new ArrayList<>() : assertions;
                    assertions.addAll(new HttpSampler.AssertionsBuilder(null).statusCode(200).build());
                    sampler.getRunning().setAssert_(assertions);
                    chain.doRun(ctx);
                }
            }));

        http("get", request -> request.get("/get"));
        http("post", request -> request.post("/post").body(""));
        http("put", request -> request.put("/put").body(""));
        http("delete", request -> request.delete("/delete"));
        http("patch", request -> request.patch("/patch").body(""));
        http("head", request -> request.head("/head"));
        http("options", request -> request.options("/options"));
        http("trace", request -> request.trace("/trace"));
        http("connect", request -> request.connect("/connect"));

        for(String method : methodValues) {
            http(method, request -> {
                request.url("/" + method).method(method);
                if ("post".equals(method) || "put".equals(method) || "patch".equals(method)) {
                    request.body("");
                }
            });
        }

    }

    private void registerStub(String method) {
        WireMock.stubFor(WireMock
            .request(method, WireMock.urlEqualTo("/" + method.toLowerCase()))
            .willReturn(WireMock.ok()));
    }

}
