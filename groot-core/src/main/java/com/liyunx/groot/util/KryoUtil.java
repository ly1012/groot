package com.liyunx.groot.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * kryo 工具类
 */
public class KryoUtil {

    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        return kryo;
    });

    public static Kryo getKryo() {
        return kryoThreadLocal.get();
    }

    /**
     * 返回一个对象的深拷贝。
     *
     * <p>以下情况会深拷贝失败或数据错误：</p><ul>
     *
     * <li>【匿名类】在非静态方法中使用匿名类。<br/>
     * 比如下面的例子：ArrayList 双花括号写法（匿名内部类）。
     * 此时匿名类没有无参构造器，只有一个 <code>package.EnclosingClass$Anonymous(package.EnclosingClass)</code> 构造器。
     * Kryo 首先尝试使用无参构造器实例化对象，明显会失败，然后使用回退的策略，不使用任何构造器进行实例化，
     * 此时得到的匿名类新对象的 elementData 为 null（ArrayList 无参构造器的 elementData 初始化代码未被执行），调用 add 方法时发生 NPE。
     * <pre>{@code
     * public class AnonymousClassTest {
     *
     *     public static void main(String[] args) {
     *         // 在非静态方法中使用匿名类
     *         // Constructors Count: 1
     *         // package.AnonymousClassTest$3(package.AnonymousClassTest)
     *         new AnonymousClassTest().noStaticMakeList();
     *     }
     *
     *     private List<String> noStaticMakeList() {
     *         return new ArrayList<String>(){{
     *             add("one");
     *         }};
     *     }
     *
     * }
     * }</pre>
     * </li>
     * <li>【匿名类】在静态方法中使用匿名类，copy 之后的数据不一致。
     * <pre>{@code
     * public static void testAnonymousArrayList() {
     *     List<Map<String, Object>> copied = new ArrayList<Map<String, Object>>() {{
     *         add(new HashMap<String, Object>() {{
     *             put("forKey1", "forOne");
     *             put("forKey2", "forTwo");
     *         }});
     *         add(new HashMap<String, Object>() {{
     *             put("forKey1", "一");
     *             put("forKey2", "二");
     *         }});
     *     }};
     *     assert KryoUtil.copy(copied).size() == 4;
     * }
     * }</pre>
     * </li>
     *
     * </ul>
     *
     * @param object 原对象
     * @param <T>    原对象类型
     * @return 原对象的深拷贝
     */
    public static <T> T copy(T object) {
        return kryoThreadLocal.get().copy(object);
    }

}
