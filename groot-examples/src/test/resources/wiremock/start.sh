# 启动一个 WireMock 服务，方便使用 WireMock UI 对 Mock 规则进行编辑
# https://github.com/ly1012/wiremock-ui
# 注意这里的端口号和单元测试代码中的端口号不同，防止冲突，如此该服务便可后台常驻，
java -jar /Users/yun/Downloads/test/wiremock/wiremock-jre8-standalone-2.35.2.jar \
  --global-response-templating -port=8235 --https-port=8443 --container-threads=20

