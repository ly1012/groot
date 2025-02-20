package com.liyunx.groot.testng.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识一个 @Test 方法内可以使用 Groot API，比如
 * <pre><code>
 *     $http("访问首页", it -> it
 *         .get("/order/10")
 *     );
 * </code></pre>
 *
 * <p>@Test 方法执行前创建 session，执行后销毁 session</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface GrootSupport {

}
