package com.liyunx.groot.protocol.http.support;

import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.util.KryoUtil;
import groovy.lang.GString;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Http Model 业务支持类
 */
public class HttpModelSupport {

    /**
     * Body Copy（Request.Body/MultiPart.Body/Response)
     *
     * @param body     Body
     * @param location Body 位置，如 http.binary
     * @return Body 深拷贝
     */
    public static Object bodyCopy(Object body, String location) {
        Object res;
        if (body instanceof String) {
            res = body;
        } else if (body instanceof GString) {
            res = body.toString();
        } else if (body instanceof byte[]) {
            byte[] originBody = (byte[]) body;
            res = Arrays.copyOf(originBody, originBody.length);
        } else if (body instanceof File) {
            res = new File(((File) body).getPath());
        } else {
            res = KryoUtil.copy(body);
            //throw new InvalidDataException(
            //    "%s，期望类型：byte[]/File/stringToFileOrByteArray，实际类型：%s",
            //    location,
            //    body.getClass().getName());
        }
        return res;
    }

    /**
     * 多值参数类集合的合并
     *
     * @param lower      低优先级的集合
     * @param higher     高优先级的集合（同名的以此为准，覆盖低优先级）
     * @param nameGetter 获取元素名称的函数，用于同名判断
     * @param <Manager>  元素集合类
     * @param <E>        元素类
     * @return 合并后的集合
     */
    public static <Manager extends Copyable<Manager> & List<E>, E extends Copyable<E>> Manager multiValueManagerMerge(
        Manager lower,
        Manager higher,
        Function<E, String> nameGetter) {

        if (higher == null) {
            return lower.copy();
        }

        // lower    higher    merged
        //          a: ao1    a: ao1
        //          a: ao2    a: ao2
        // b: bt1   b: bo     b: bo
        // b: bt2
        // c: ct1   c: co1    c: co1
        //          c: co2    c: co2
        // d: dt              d: dt
        // 观察上面的合并策略，可以看出当以 higher 为基准进行合并时，
        // 只需要将 lower 中有而 higher 中没有的 name 加入 higher 即可完成合并。
        Manager res = higher.copy();
        for (E l : lower) {
            // 查询当前 name 是否存在于 higher 中
            String lName = nameGetter.apply(l);
            if (lName == null || lName.isEmpty())
                throw new InvalidDataException(
                    "Element name can't be empty. \nClass: %s. \nElement: %s",
                    l.getClass().getName(),
                    l.toString());

            boolean found = false;
            for (E h : higher) {
                String hName = nameGetter.apply(h);
                if (hName == null || hName.isEmpty())
                    throw new InvalidDataException(
                        "Element name can't be empty. \nClass: %s. \nElement: %s",
                        h.getClass().getName(),
                        h.toString());

                if (lName.equalsIgnoreCase(hName)) {
                    found = true;
                    break;
                }
            }

            // 如果当前 name 在 higher 中不存在，则加入合并数据
            if (!found) {
                res.add(l.copy());
            }
        }
        return res;
    }

}
