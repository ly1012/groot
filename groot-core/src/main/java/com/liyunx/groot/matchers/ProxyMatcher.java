package com.liyunx.groot.matchers;

import com.alibaba.fastjson2.JSON;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.processor.assertion.Assertion;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * 代理 Matcher，为了支持动态表达式而设计，该类设计上是 Assertion 的辅助类，所以同样需要是线程安全类。
 *
 * <p>同标准 {@link Matcher} 一样，ProxyMatcher 应当设计为不可变和幂等，但由于 Matcher 的设计和 ProxyMatcher 的设计有些小冲突，
 * 导致直接执行 ProxyMatcher 在某些情况下会出现无法预期的行为。
 *
 * <p><b>下面给出两个解决方案（需要配合 MatcherAssertion 子类使用）：</b>
 *
 * <p>方案一：也是推荐的使用方案（没有潜在问题）。
 * 不直接执行 ProxyMatcher，而是调用 {@link #toMatcher(ContextWrapper)} 方法，返回标准 Matcher 对象（不可变对象），然后再执行。
 * 缺点是不能在标准 Matcher 中嵌套 ProxyMatcher，比如 <code>Matchers.allOf(ProxyMatchers.equalTo("${name}"))</code>，
 * 支持在 ProxyMatcher 中嵌套标准 Matcher。
 * 即无法在已有的组合类标准 Matcher 中嵌套 ProxyMatcher，比如 allOf/anyOf 等等，其他情况都可以支持。
 *
 * <p>方案二：直接将 ProxyMatcher 当作标准 Matcher 使用（某些场景下会出现无法预期的行为）。
 *
 * <p>我们知道，标准 Matcher 是不可变的，一个标准 Matcher 对象可以在多个线程中多次执行。而 ProxyMatcher 的数据是动态计算后生成的，
 * 这意味着 ProxyMatcher 从需求上就表现为可变。
 *
 * <p>当然我们也不是毫无办法，通过 ThreadLocal 获取当前的上下文对象，我们可以在 Matcher 执行期间动态生成标准 Matcher 对象后执行。
 * 那么我们上面说的冲突的点在哪里呢？这就不得不说下 Matcher 的设计了，Matcher 的 matches 方法和 describe 方法是分开执行的，比如现在有
 * <code>matcher = allOf(matcher1, matcher2, matcher3) </code>对象，
 * 先调用 <code>matcher.matches(actual)</code> 方法，再调用 <code>matcher.describe(...)</code> 方法。
 *
 * <p>对 ProxyMatcher 来说，标准 Matcher 对象是动态生成的，因此需要在两个方法中共享一个 Matcher 对象。另外还有一个问题，
 * 比如诸如 {@link org.hamcrest.DiagnosingMatcher} 这类的实现，当断言失败时，describe(...) 方法会再次调用 matches 方法。
 * 可以看出，一个 Matcher 对象在断言时，其 matches 方法和 describe 方法可能在不同位置被多次调用。
 *
 * <p>下面我们尝试解决共享对象的问题，在一个 ProxyMatcher 对象的两个方法中共享 Matcher 对象。
 * 最简单的设计自然是使用非静态成员变量，但是这里不行，无法保证线程安全，另外可能先调用两次 matches，再调用两次 describe，会覆盖之前的值。
 * 既然要保证线程安全，在不使用锁的情况下，我们用 ThreadLocal 试试，这样可以保证每个线程存储了一份 Matcher 对象，
 * 但有多个 ProxyMatcher 对象，所以现在需要每个线程的每个 ProxyMatcher 对象有一份 Matcher 对象。
 * 问题都解决了吗？还没有，即使在同一个线程中，一个 ProxyMatcher 对象也可能调用两次，但我们能做到的最小 ID 是 ProxyMatcher 对象本身，
 * 换句话说，在一个线程内的一次执行过程中，只有当一个 ProxyMatcher 对象仅关联到一个 Matcher 对象时，才能保证线程安全和多次执行。
 * 总结一下，一个线程内的一次执行过程中（MatcherAssertion），每个 ProxyMatcher 对象仅能对应一个确定的 Matcher 对象。
 *
 * <p>目前支持两种用例形式，代码用例和配置用例。
 * 配置用例反序列化时生成的对象都是新的 Matcher/ProxyMatcher 对象，不会出现一个 ProxyMatcher 对象在一个 MatcherAssertion 中出现两次。
 * 第二种方案可以满足配置用例，即 JSON 反序列化时，可以返回 Matcher/ProxyMatcher 对象，不用担心执行出现问题。
 * 代码用例中 Matcher/ProxyMatcher 对象是用户自己传的，因此需要考虑上面的问题（后续如果有精力，
 * 理论上可以将所有标准 Matcher 都包装为支持动态表达式的 ProxyMatcher）。
 * <p>
 * 代码用例中使用 Matcher 推荐规范：
 * <ul>
 *     <li>不需要动态表达式支持：直接使用标准 Matcher(可以引用已存在对象)</li>
 *     <li>需要动态表达式支持：</li>
 *     <ul>
 *         <li>支持 ProxyMatcher 嵌套 ProxyMatcher(可以引用已存在对象)</li>
 *         <li>支持 ProxyMatcher 嵌套标准 Matcher(可以引用已存在对象)</li>
 *         <li>支持标准 Matcher 嵌套标准 Matcher(可以引用已存在对象)</li>
 *         <li>不支持标准 Matcher 嵌套 ProxyMatcher，但支持以下特殊情况：</li>
 *         <ul>
 *             <li>一个 MatcherAssertion 对象中，不存在重复的 ProxyMatcher 对象（即同一对象在任意位置都不会出现两次），
 *             最保险的写法就是不要引用已存在的 ProxyMatcher 对象。如果一个 MatcherAssertion 对象中，同个 ProxyMatcher 对象出现两次，
 *             比如 Matchers.allOf(proxyMatcher, proxyMatcher)，预期两处位置计算后返回的是两个不同的标准 Matcher 对象，
 *             但实际上返回的是同一个 Matcher 对象，从而可能会出现一些无法预期的行为。</li>
 *         </ul>
 *     </ul>
 * </ul>
 */
@SuppressWarnings("rawtypes")
public abstract class ProxyMatcher<T> extends BaseMatcher<T> {

    protected List<Class> matcherValueTypes;

    /**
     * {@link Assertion#process(ContextWrapper)} 方法中进行 set 和 remove
     */
    public static final ThreadLocal<ContextWrapper> ctxThreadLocal = new ThreadLocal<>();
    public static final ThreadLocal<Map<ProxyMatcher, Matcher>> matcherThreadLocal = new ThreadLocal<>();

    /**
     * 计算动态表达式，并生成标准的 {@link Matcher} 对象
     *
     * @param ctx 上下文对象
     * @return {@link Matcher} 对象
     */
    public abstract Matcher<T> toMatcher(ContextWrapper ctx);

    @Override
    public final boolean matches(Object actual) {
        // 使用生成的标准 Matcher 对象进行断言
        return getMatcherInstance().matches(actual);
    }

    @Override
    public final void describeTo(Description description) {
        getMatcherInstance().describeTo(description);
    }

    private Matcher getMatcherInstance() {
        // 获取已存在 Matcher
        Map<ProxyMatcher, Matcher> cache = matcherThreadLocal.get();
        if (cache == null) {
            throw new IllegalStateException("请先调用 matcherThreadLocal.set(new HashMap<>()) 方法");
        }
        Matcher matcher = cache.get(this);

        // WARNING:
        // 这里允许多次调用，但只会持有一个 Matcher 对象（一个 ProxyMatcher 对象多次调用，但标准 Matcher 只会在第一次生成）
        // 因为去掉了 fail-fast 代码，可能会出现无法预期的行为，见类注释，Matcher 嵌套 ProxyMatcher 时尽量避免引用已存在的 ProxyMatcher 对象

        // 一次执行过程中，只有第一次时生成，如果已存在则直接返回
        if (matcher != null) {
            return matcher;
        }

        // 动态计算，返回一个 Matcher
        ContextWrapper ctx = ctxThreadLocal.get();
        if (ctx == null) {
            throw new IllegalStateException("请先调用 ctxThreadLocal.set(ctx) 方法");
        }
        matcher = toMatcher(ctx);
        cache.put(this, matcher);
        return matcher;
    }

    protected Class<T> getFirstClass() {
        //noinspection unchecked
        return (isNull(matcherValueTypes) || matcherValueTypes.isEmpty())
            ? null
            : matcherValueTypes.get(0);
    }

    @SuppressWarnings("unchecked")
    public static <T> T valueAsType(Class<T> matcherValueType, Object value) {
        if (matcherValueType == null || matcherValueType.isInstance(value)) {
            return (T) value;
        }

        Object valueAsType;
        if (matcherValueType.equals(Integer.class)) {
            valueAsType = Integer.valueOf(String.valueOf(value));
        } else if (matcherValueType.equals(Long.class)) {
            valueAsType = Long.valueOf(String.valueOf(value));
        } else {
            valueAsType = JSON.parseObject(JSON.toJSONString(value), matcherValueType);
        }
        return (T) valueAsType;
    }

    @SuppressWarnings("unchecked")
    public static <T> Matcher<T> toMatcherIfProxy(ContextWrapper ctx, List<Class> matcherValueTypes, Matcher matcher) {
        if (matcher instanceof ProxyMatcher proxyMatcher) {
            // 仅当没有指定预期值类型，且存在默认预期值类型的情况下，才传递预期值类型
            if (isBlank(proxyMatcher.matcherValueTypes) && !isBlank(matcherValueTypes)) {
                proxyMatcher.matcherValueTypes = matcherValueTypes;
            }
            return proxyMatcher.toMatcher(ctx);
        } else {
            return matcher;
        }
    }

    private static boolean isBlank(List<Class> valueTypes) {
        if (valueTypes == null || valueTypes.isEmpty()) {
            return true;
        }

        for (Class valueType : valueTypes) {
            if (nonNull(valueType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 根据实际值计算默认预期值类型
     *
     * @return 默认预期值类型，可能值：基本类型、String 类型、null
     */
    public static Class<?> matcherValueType(Object actual) {
        // 使用频率高的先判断
        // String 类型
        if (actual instanceof String)
            return String.class;

        // 基本数据类型
        if (actual instanceof Integer)
            return Integer.class;

        if (actual instanceof Long)
            return Long.class;

        if (actual instanceof Double)
            return Double.class;

        if (actual instanceof Boolean)
            return Boolean.class;

        if (actual instanceof Byte)
            return Byte.class;

        if (actual instanceof Short)
            return Short.class;

        if (actual instanceof Float)
            return Float.class;

        if (actual instanceof Character)
            return Character.class;

        // 其他类型，不支持默认预期值类型
        return null;
    }

    public List<Class> getMatcherValueTypes() {
        return matcherValueTypes;
    }

    public void setMatcherValueTypes(List<Class> matcherValueType) {
        this.matcherValueTypes = matcherValueType;
    }
}
