package com.liyunx.groot.support;

import com.liyunx.groot.processor.extractor.Extractor;

import java.util.Objects;

/**
 * 包装一个引用类型的值
 *
 * <p>应用场景：Lambda 表达式
 * <pre><code>
 * Ref<Integer> count = ref(0);
 * int count2 = 0;
 * Consumer<String> consumer = input -> {
 *     count.value = input.length();
 *
 *     // Compile Error
 *     // Variable used in lambda expression should be final or effectively final
 *     //count2 = input.length();
 * };
 * System.out.println(count);
 * </code></pre>
 *
 * <p>应用场景：{@link Extractor}
 * <pre><code>
 * Ref<Integer> count = ref(0);
 * httpWith("Get 请求", http -> http
 *     .request(request -> request
 *         .get("/get"))
 *     .extract(extract -> extract
 *         .jsonpath(count, "$.data.total")));
 * if (count.value == 0) {
 *     System.out.println("查询结果为空");
 * }
 * </code></pre>
 *
 * @param <T> 值类型
 */
public class Ref<T> {

    public T value;

    private Ref() {
    }

    private Ref(T value) {
        this.value = value;
    }

    public static <T> Ref<T> ref() {
        return new Ref<>();
    }

    public static <T> Ref<T> ref(T value) {
        return new Ref<>(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ref<?> that = (Ref<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

}
