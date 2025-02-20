package com.liyunx.groot.testelement;

import com.alibaba.fastjson2.annotation.JSONType;
import com.liyunx.groot.SessionRunner;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.common.Validatable;
import com.liyunx.groot.dataloader.fastjson2.deserializer.TestElementObjectReader;
import com.liyunx.groot.filter.TestFilter;

/**
 * 测试元件是能根据其父上下文链独立执行的一个逻辑执行单元。
 * 每个测试元件都具有唯一的 {@link KeyWord} 关键字，用于识别配置风格中组件的类型（代码风格中组件类型是已知的）。
 *
 * <p>测试元件是整个工具的核心，
 * 环境管理、变量管理、前置处理器、后置处理器、插值表达式、函数、断言、配置继承、取样器、控制器、用例引用等功能都将围绕测试元件展开。
 *
 * <p>HTTP 请求、循环控制器、测试用例、用例引用步骤等都是测试元件。一个测试元件可以包含多个子测试元件，如测试用例、控制器。
 *
 * <p>设计开发一个新的测试元件前，应当先明确该测试元件要实现的功能、需要的参数，是否和其他测试元件功能重合，实现粒度，然后再编码实现。
 *
 * <p>important! TestElement 为非线程安全类，应当在单个线程中运行（不要在多个线程间共享 TestElement 对象）。
 * 如果要在多个线程中运行同一个 TestElement 对象，请先 {@link #copy()}，每个线程使用该 TestElement 的拷贝进行运行。
 *
 * <p>important! TestElement 中有两类数据，一类是声明时数据，一类是运行时数据（使用一个新对象来表示）。
 * 声明时数据只读处理，运行时数据可在运行期间修改。如果测试元件不希望某个声明时数据在运行期被修改，应当直接使用声明时数据，而非运行时数据。
 * 声明时数据保存了 TestElement 的原始数据，运行时数据保存了本次测试元件执行时的数据。
 * 除了构建对象外，禁止修改 TestElement 的声明时数据，如果对声明时数据进行修改，会导致循环执行该 TestElement 时出错。
 * 比如 name 字段声明时的值为 login baidu ${cnt}，如果执行 TestElement 时直接修改，如
 * rawName = evalAsString(rawName)，那么将导致不管执行几次， name 值始终是 login baidu 1，
 * 而不是第一次 login baidu 1，第二次是 login baidu 2。
 * <p>为什么存在运行时数据？因为 {@link TestFilter} 允许通过修改运行时数据来改变测试行为，比如加解密、增加默认断言、修改元件名称等等。
 *
 * <p><br>实现类需要自行调用 eval 方法完成动态数据的计算（一般是模板计算，如果该字段支持模板）。
 * 工具无法提前计算，原因有二：
 * <ol>
 * <li> 字段本身不能提前计算，否则会破坏原有功能 </li>
 * <li> 框架不能获知哪些字段需要被计算，哪些字段不需要被计算 </li>
 * </ol>
 */
@JSONType(deserializer = TestElementObjectReader.class)
@FunctionalInterface
public interface TestElement<T extends TestResult<T>>
    extends Validatable, Copyable<TestElement<T>> {

    /**
     * 运行测试元件
     * <p>
     * 用户应避免直接调用该方法，推荐使用 {@link SessionRunner#run} 方法。
     *
     * @param session 每个测试用例使用各自的 SessionRunner
     * @return 执行结果
     */
    T run(SessionRunner session);

    /**
     * 本方法用于解决元件对象的线程安全问题。
     * TestElement 是有状态的，如果设计成无状态的（线程安全类，属性只读/安全修改 + 方法传参）则不需要 copy 方法。
     * <p>
     * 考虑到设计成无状态的编码成本和调试成本更高，且绝大部分场景无需并发执行同一对象，故这里采用有状态的设计。
     * 如果需要并发执行，需要执行 copied 对象。
     * 场景示例：用例数据驱动，每组数据并发运行时应该使用 testCase.copy() 对象，而非直接使用 testCase 对象。
     * <p>
     * 如果默认实现不满足线程安全最小拷贝，则需要重写该方法。一般情况下，只需要简单的将当前对象的声明时数据赋值给新对象即可。
     * 这里其实涉及到两个概念，数据类和逻辑类，测试元件类 = 数据类（声明时数据，只读） + 逻辑类。
     * 测试元件类的线程不安全来自逻辑类，逻辑类的线程不安全来自不安全地使用成员变量，
     * 所以通过简单的拷贝，让每个线程各自持有一个包含相同声明时数据的测试元件类对象，即可保证线程安全。
     * <p>
     * TODO 有状态设计的缺点在于，并发执行需要 copy，不太符合用户习惯，后面有时间写个无状态版本对比下？
     *
     * @return 对象的拷贝（不是深拷贝，也不是浅拷贝，是保证线程安全的最小拷贝）
     */
    @Override
    default TestElement<T> copy() {
        // 默认认为当前类是线程安全的，否则应当重写该方法
        return this;
    }

}
