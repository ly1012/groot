package com.liyunx.groot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关键字，对应 JSON 中的 Key。每个测试元件或配置元件的关键字在其元件类型范围内必须是唯一的，如测试元件中每个测试元件的关键字必须不同。
 *
 * <p><br>TestElement 示例：
 * <blockquote><pre>
 *{@code
 * @KeyWord("if")
 * public class IfController extends AbstractController {
 *
 *   @JSONField(name = "if")
 *   String condition;
 *
 *   ...
 * }
 * }
 * </pre></blockquote>
 *
 * <p><br>比如下面的 Yaml 数据将被反序列化为 IfController 对象
 * <blockquote><pre>
 *   - name: 条件控制器
 *     if: "true"
 * </pre></blockquote>
 *
 * <p>而包含 testcase key 的 Yaml 数据将被反序列化为 TestCaseIncludeController 对象
 * <blockquote><pre>
 *   - name: 引用其他测试用例
 *     testcase: testcases/login.yml
 * </pre></blockquote>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface KeyWord {

  String miss = "__miss__";

  String value() default miss;

  /**
   * 不需要进行注册请设置 true，如 TestCase、HttpServiceConfigItem 等
   */
  boolean ignore() default false;

}
