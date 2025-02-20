package com.liyunx.groot.processor.assertion;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.dataloader.fastjson2.deserializer.AssertionObjectReader;
import com.liyunx.groot.processor.PostProcessor;

/**
 * 断言接口。断言失败应当抛出断言错误 {@link java.lang.AssertionError}。
 *
 * <p><br/>断言接口基于断言类型设计。这样的设计适合扩展新的断言类型，断言类也可以具备任意的属性，具有较高的灵活性。
 * 但以代码形式编写用例时，基于值类型来进行断言有时会更加方便，比如对一个值进行多次断言，比如根据值类型自动补全相应的断言方法。
 * 因此某些场景下更推荐直接使用 AssertJ 或 Hamcrest。来看一个示例：<br/>
 * <p>
 * 基于断言类型的断言写法：
 * <pre><code>
 * startsWith("assertj", "ass");
 * contains("assertj", "rt");
 * hasSize("assertj", 7);
 * </code></pre>
 * <p>
 * 基于值类型的断言写法：
 * <pre><code>
 * assertThat("assertj").startsWith("ass").contains("rt").hasSize(7);
 * </code></pre>
 * <p>
 * <p>
 * <p>
 * TODO Assertion 暂时是没加 AssertResult 返回的，默认断言失败，用例直接终止。<br/>
 * TODO 之所以这样设计，还是因为关注点在自动化测试，而非性能测试。自动化测试断言失败，则后续测试不再有意义应该终止；
 * 而性能测试为了获取压力指标数据，而非功能验证，故接口报错仍应继续执行，如 200 并发执行用例 10 分钟。
 * 另一个考虑因素是 PostProcessor/Assertion/Extractor 为函数式接口，方便用例中添加自定义处理逻辑，如果强制要求返回值，
 * 如果返回 null 没有意义，如果返回一个标准的 AssertResult/ExtractResult，用户需要添加对应字段信息，增加用例编码负担。
 * 比如:
 * <pre><code>
 *     // 本来一句代码即可
 *     Assertions.assertThat(100).isEqualTo(100);
 *
 *     // 现在需要不少额外的代码
 *     AssertionResult r = new AssertionResult();
 *     r.setItem("两个数值是否相等");
 *     r.setActual(100);
 *     r.setExpected(100);
 *     try {
 *         Assertions.assertThat(100).isEqualTo(100);
 *         r.setStatus("成功");
 *     } catch(AssertionError error) {
 *         r.setStatus("失败");
 *         r.setErrorMessage(error.getMessage());
 *         r.setError(e);
 *     }
 *     return r;
 * </code></pre>
 *
 * <p>如果要收集断言结果或增加软断言功能，可以采用以下方案：<br/>
 *
 * <ul>
 *   <li>PostProcessor 仍然使用统一且唯一的接口方法，没有 AssertionResult 返回值。</li>
 *   <li>Assertion 实现类通过 TestResult 存储本断言的 AssertionResult 数据，原代码不做修改。</li>
 *   <li>可在 run 方法中对 Assertion 进行处理，如 catch 实现软断言，或收集处理断言结果。</li>
 *   <li>可在监听器中通过 TestResult 获取断言结果。</li>
 * </ul>
 * <p>
 * 一个初步的代码设计：
 *
 * <ul>
 *   <li>AbstractTestElement 每次执行 Assertion 前调用 setCurrentAssertionResult(null) 初始化当前断言结果</li>
 *   <li>Assertion 断言失败时抛出 AssertionError 或调用 TestResult.addAssertionResult(r) 或者两者皆有 </li>
 *   <li>TestResult.addAssertionResult 方法内部判断如果 currentAssertionResult == null，
 *   则调用 setCurrentAssertionResult，值为 add 方法实参</li>
 *   <li>AbstractTestElement 执行 Assertion 后 catch 断言错误：
 *   <ul>
 *     <li>断言失败或断言成功：</li>
 *     <ul>
 *       <li>currentAssertionResult == null，创建新的断言结果对象，调用 add 方法</li>
 *       <li>否则，不做处理</li>
 *     </ul>
 *     <li>根据断言模式立即终止用例或元件执行结束后终止或用例结束后终止或用例不终止</li>
 *   </ul>
 *   </li>
 * </ul>
 *
 */
@JSONType(deserializer = AssertionObjectReader.class)
@FunctionalInterface
public interface Assertion extends PostProcessor {

    /**
     * 执行断言
     */
    void process(ContextWrapper ctx);

}
