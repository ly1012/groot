
> [!TIP]
> 当前版本完整实现了核心功能，但生态还较弱。欢迎提交 Pull 请求、扩展组件或 Issues。

Groot 是一款基于 Java 生态构建的轻量级自动化测试工具，为测试人员和技术团队提供高效的自动化能力支撑。通过多种形式的用例支持与简洁的设计理念，帮助快速落地自动化测试、数据工厂构建、接口健康巡检、测试平台开发等核心质量保障场景。

项目提供了多个扩展点，可通过扩展实现功能增强，比如协议扩展、控制器扩展、认证与加解密扩展、断言扩展、报告扩展、函数扩展等等。

同时支持代码风格用例（Java/Groovy）和配置风格用例（Yaml/Json）。代码风格用例推荐 Groovy，配置风格用例推荐 Yaml。

## 下载与文档

本项目为标准 Java 项目，可通过 Maven/Gradle 下载依赖。

帮助文档：[GitHub Pages 访问](https://ly1012.github.io/groot-docs)、[国内站点](https://liyunx.com/groot-docs)。

使用示例：参考源码中的单元测试。

## 用例演示

**Yaml 用例**

```yaml
name: MultiPart 测试用例
steps:
  - name: 上传多个文件
    http:
      url: /multipart/upload
      method: POST
      multipart:
        - file: data/降龙十八掌.txt
        - file: data/独孤九剑.txt
    validate:
      - statusCode: 200
```

**Json 用例**

```json
{
  "name": "MultiPart 测试用例",
  "steps": [
    {
      "name": "上传多个文件",
      "http": {
        "url": "/multipart/upload",
        "method": "POST",
        "multipart": [
          {
            "file": "data/降龙十八掌.txt"
          },
          {
            "file": "data/独孤九剑.txt"
          }
        ]
      },
      "validate": [
        {
          "statusCode": 200
        }
      ]
    }
  ]
}
```

**Java 用例**

方法命名规范：

- 方法名为关键字或变形关键字：不包含配置和前后置。比如直接发送请求，并在 then 回调中提取或断言响应内容。
- 方法名为关键字或变形关键字 + With：包含配置和前后置。比如对请求进行额外的配置（比如设置代理）、增加提取操作和断言。

测试框架为 TestNG，依赖 groot-testng 模块。

```java
public class PostTest extends GrootTestNGTestCase {

    @GrootSupport
    @Test(description = "MultiPart：上传多个文件")
    public void testPost() {
        http("上传多个文件1", request -> request
            .post("/multipart/upload")
            .multiPartFile("data/降龙十八掌.txt")
            .multiPartFile("data/独孤九剑.txt")
        ).then(r -> {
            Assertions.assertThat(r.getResponse().getStatus()).isEqualTo(200);
        });

        httpWith("上传多个文件2", action -> action
            .request(request -> request
                .post("/multipart/upload")
                .multiPartFile("data/降龙十八掌.txt")
                .multiPartFile("data/独孤九剑.txt"))
            .validate(validate -> validate
                .statusCode(200)));
    }

}
```

**Groovy 用例**

编码形式的用例推荐使用 Groovy 风格用例，易于书写和阅读。

```groovy
class PostGroovyTest extends GrootTestNGTestCase {

    @GrootSupport
    @Test(description = "MultiPart：上传多个文件")
    void testMultiPart_MultiFile() {
        http("上传多个文件1") {
            post "/multipart/upload"
            multiPartFile "data/降龙十八掌.txt"
            multiPartFile "data/独孤九剑.txt"
        }.then {
            Assertions.assertThat(it.response.status).isEqualTo(200)
        }

        httpWith("上传多个文件2") {
            request {
                post "/multipart/upload"
                multiPartFile "data/降龙十八掌.txt"
                multiPartFile "data/独孤九剑.txt"
            }
            validate {
                statusCode 200
            }
        }
    }

}
```

## 二次开发

基于 Groot 进行定制化开发，通常不需要修改 Groot 项目源码。Groot 采用插拔式插件设计，我们可以新建一个插件（扩展）项目，并根据扩展点规范完成扩展组件开发，用户引入新的扩展组件依赖即可完成功能增强。

Groot 提供的是自动化基础能力，你可以基于此进行更高维度的封装。

详情见帮助文档中的 `使用指南 - 扩展与开发`。

