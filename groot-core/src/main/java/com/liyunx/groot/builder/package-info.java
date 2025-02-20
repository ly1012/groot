/**
 * ExtensibleXXXBuilder 扩展类（覆盖层）
 *
 * <p>使用方式：
 * <ul>
 *     <li>自动扩展：
 *         <ol>
 *             <li>扩展代码：为了方便测试和防止类膨胀，扩展代码应该写在 src/test/java 目录的 com.liyunx.groot.builder 下，
 *             这样项目中同名扩展类最多只有两个（core 包中的一个，项目中的一个）</li>
 *             <li>资源生成：通过 Maven 插件，在扩展组件 Jar 打包阶段，将所有扩展代码打包进 src/main/resources 资源目录下的
 *             groot/builder 文件夹下，资源文件名称为扩展类名 </li>
 *             <li>源码生成：通过 Maven 插件，自动扫描所有依赖的资源目录，并生成 ExtensibleXXXBuilder 类源码到当前项目的
 *             src/main/java 的 com.liyunx.groot.builder 下。或手动调用工具类方法生成类的源码。 </li>
 *         </ol>
 *     </li>
 *     <li>手动扩展（不推荐）：通过类覆盖，手动在同名的该类中增加入口方法</li>
 * </ul>
 *
 * <p>设计目的：
 *
 * <p>每个自动化测试项目所依赖的扩展组件的数量和类型是不确定的，也可能依赖的是公司私有的扩展组件，因此 Groot 无法直接提供一个确定的用户入口类。
 * 也就是说，入口类提供的能力应该由项目依赖决定，它是可变的。另一方面，我们无法在某一个非用户 Jar 中声明入口类的所有能力。
 *
 * <p>有没有其他方法解决该问题呢？也有，那就是不使用入口类，而是用户传入，比如 <code>apply(key, configItem)</code> 或
 * <code>apply(JsonPathExtractor.of("name", "$.jsonpath"))</code>，但这明显不符合 Groot 简化用例编写的设计初衷。
 *
 * <p>Groot 对该问题的解决方案是使用扩展类 ExtensibleXXX，它被设计用于将项目依赖的所有扩展组件（比如 groot-http）的能力集成进这些扩展类，
 * 而入口类（比如 AllConfigBuilder 等等）继承自这些扩展类，达到修改入口能力的效果（入口能力可插拔）（使用类覆盖，不修改原代码）。
 */
package com.liyunx.groot.builder;