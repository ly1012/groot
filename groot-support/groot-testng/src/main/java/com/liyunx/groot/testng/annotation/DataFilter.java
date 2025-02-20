package com.liyunx.groot.testng.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataFilter {

    /**
     * 过滤行：参数化数据仅包含以下行（1-based），注意值为字符串
     * <code>seq: "[2..-1]"</code>
     * <p>
     * 其他示例：
     * <pre><code>
     * [1..3]
     * [1..]
     * [..4]
     * [1, 2, 3]
     * [1, 3, -4, -1]
     * </code></pre>
     */
    String slice() default "";

    /**
     * 过滤行：参数化数据仅包含满足表达式的行
     *
     * <p>
     * 示例数据 test.json：
     * <pre> {@code
     * [
     *   {"username": "admin", "password": "123456"},
     *   {"username": "user", "password": "666666"}
     * ]
     * }
     * </pre></p>
     *
     * <p>
     * 参数类型：仅有一个参数且参数类型为 Map，Key 为变量名
     * <pre>{@code
     *     @GrootDataSource(value = "test.json", expr = "username.contains('min') && password <= 333333")
     *     @Test
     *     public void testGrootDataSource(Map<String, Object> data) {
     *     }
     * }</pre>
     * </p>
     *
     * <p>
     * 参数为其他类型：所有参数（Object[]）的变量名为 data ，第 N 个参数的变量名为 pn
     * <pre>{@code
     *     @DataFilter(expr = "p2 < 333333")
     *     @Test(dataProvider = "testData")
     *     public void testGrootDataSource(String username, int password) {
     *     }
     *
     *     @DataProvider(name = "testData")
     *     public Object[][] dp() {
     *         return new Object[][]{
     *             {"admin", 123456},
     *             {"user", 666666}
     *         };
     *     }
     * }</pre></p>
     *
     * @return Groovy 表达式
     */
    String expr() default "";

    // 过滤列：暂不支持

}
