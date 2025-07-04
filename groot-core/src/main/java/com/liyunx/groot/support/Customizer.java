package com.liyunx.groot.support;

/**
 * 函数式接口，方便配置构建，如下所示：
 *
 * <pre>{@code
 * // 声明一个函数
 * Customizer<HttpRequestBuilder> request = it -> it
 *     .withService("userCenter")
 *     .pathVariable("id", "11")
 *     .queryParam("id", "1")
 *     .header("Username", "tom", "jack")
 *     .get("/get");
 *
 * // http 方法的第二个参数为 Customizer<HttpRequestBuilder>
 * http("Get 请求", it -> {
 *     it.get("https://httpbin.org/get");
 * }).then("响应时间应该小于 100 ms", r -> {
 *     assertThat(r.getTime()).isLessThan(100);
 * }).then("打印分段耗时和报文大小信息", r -> {
 *     r.getStat().prettyPrint();
 * });
 * }</pre>
 *
 * @param <T> Builder 类
 */
@FunctionalInterface
public interface Customizer<T> {

    /**
     * 通过 Builder 对象 it 进行对象自定义构建
     *
     * @param it Builder 对象
     */
    void customize(T it);

    static <T> Customizer<T>[] merge(Customizer<T> beforeCustomizer,
                                     Customizer<T>[] customizers,
                                     Customizer<T> afterCustomizer) {
        int extraLength = (beforeCustomizer != null ? 1 : 0) + (afterCustomizer != null ? 1 : 0);
        Customizer<T>[] mergedClosures = new Customizer[customizers.length + extraLength];

        int offset = 0;
        if (beforeCustomizer != null) {
            mergedClosures[0] = beforeCustomizer;
            offset++;
        }
        System.arraycopy(customizers, 0, mergedClosures, offset, customizers.length);
        if (afterCustomizer != null) {
            mergedClosures[mergedClosures.length - 1] = afterCustomizer;
        }
        return mergedClosures;
    }

}
