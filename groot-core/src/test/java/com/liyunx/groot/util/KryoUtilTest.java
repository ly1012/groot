package com.liyunx.groot.util;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KryoUtilTest {

    @Test(description = "静态方法中，Kryo 无法正确拷贝匿名类")
    public static void testAnonymousArrayListWithStaticMethod() {
        List<Map<String, Object>> target = new ArrayList<>() {{
            add(new HashMap<>() {{
                put("forKey1", "forOne");
                put("forKey2", "forTwo");
            }});
            add(new HashMap<>() {{
                put("forKey1", "一");
                put("forKey2", "二");
            }});
        }};
        int copiedSize = KryoUtil.copy(target).size();
        assertThat(copiedSize).isNotEqualTo(target.size());
        assertThat(copiedSize).isEqualTo(4);
    }

    @Test(description = "非静态方法中，Kryo 无法正确拷贝匿名类", expectedExceptions = NullPointerException.class)
    public void testAnonymousArrayListWithNonStaticMethod() {
        List<Map<String, Object>> target = new ArrayList<>() {{
            add(new HashMap<>() {{
                put("forKey1", "forOne");
                put("forKey2", "forTwo");
            }});
            add(new HashMap<>() {{
                put("forKey1", "一");
                put("forKey2", "二");
            }});
        }};
        KryoUtil.copy(target);
    }

    ThreadLocal<PlainObject> threadLocal = ThreadLocal.withInitial(() -> new PlainObject("str", 10, true));
    static ThreadLocal<PlainObject> staticThreadLocal = ThreadLocal.withInitial(() -> new PlainObject("str2", 12, true));

    @Test(description = "ThreadLocal 拷贝测试", enabled = false)
    public void testThreadLocalCopy() {
        // JDK 17
        // com.esotericsoftware.kryo.KryoException:
        // java.lang.reflect.InaccessibleObjectException:
        // Unable to make field private final java.util.function.Supplier java.lang.ThreadLocal$SuppliedThreadLocal.supplier
        // accessible: module java.base does not "opens java.lang" to unnamed module @3dfada53
        KryoUtilTest copy = KryoUtil.copy(this);

        // 非静态成员变量，每次会拷贝
        assertThat(threadLocal.get()).isNotSameAs(copy.threadLocal.get());
        // 静态成员变量不受影响（因为根本就没复制，指向同一个对象）
        assertThat(staticThreadLocal.get()).isSameAs(staticThreadLocal.get());
    }

    @Test(description = "基础类型拷贝测试")
    public void testBasicType() {
        String s1 = "string value";
        String s2 = KryoUtil.copy(s1);
        assertThat(s1).isSameAs(s2);

        Long l1 = 1L;
        Long l2 = KryoUtil.copy(l1);
        assertThat(l1).isSameAs(l2);
    }


    @Test(enabled = false, description = "性能耗时参考")
    public void testPerformance() throws CloneNotSupportedException {
        long n = 1000_0000L;
        long newTime;
        long cloneTime;
        long copyTime;

        {
            // new 操作符拷贝对象
            long s = System.currentTimeMillis();
            PlainObject origin = new PlainObject();
            origin.v1 = "v1v1";
            origin.v2 = 100;
            origin.v3 = true;
            for (long i = 0; i < n; i++) {
                PlainObject obj = new PlainObject();
                obj.v1 = origin.v1;
                obj.v2 = origin.v2;
                obj.v3 = origin.v3;
            }
            newTime = System.currentTimeMillis() - s;
        }

        {
            // Object clone 方法拷贝对象
            PlainObject obj = new PlainObject("v1v1", 100, true);
            long s = System.currentTimeMillis();
            for (long i = 0; i < n; i++) {
                PlainObject object = obj.clone();
            }
            cloneTime = System.currentTimeMillis() - s;
        }

        {
            // Kryo copy 方法拷贝对象
            PlainObject obj = new PlainObject("v1v1", 100, true);
            long s = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                PlainObject obj1 = KryoUtil.copy(obj);
            }
            copyTime = System.currentTimeMillis() - s;
        }

        //System.out.println(newTime + " : " + cloneTime + " : " + copyTime);
        assertThat(copyTime).isGreaterThan(newTime * 10);
    }

    // 类必须是 public 修饰，否则 Kryo 报错 java.lang.IllegalAccessError
    public static class PlainObject implements Cloneable {

        public String v1;
        public Integer v2;
        public boolean v3;

        public PlainObject() {
        }

        public PlainObject(String v1, Integer v2, boolean v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }

        @Override
        public PlainObject clone() throws CloneNotSupportedException {
            return (PlainObject) super.clone();
        }

    }

}
