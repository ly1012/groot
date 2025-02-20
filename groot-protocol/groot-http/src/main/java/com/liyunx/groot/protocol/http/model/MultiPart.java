package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.common.Mergeable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.support.HttpModelSupport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MultiPart Body
 */
// fastjson2 Bug，目标类包含 List 子类，如 MultiPart，这里有两个 Bug，导致前两种方式都无法正常反序列化，因此暂时先使用第三种方式：
// 反序列化代码： JSON.parseObject(jsonString, HttpSampler.class)
//
// （1）默认情况（不指定自定义反序列化类）
// 当目标类嵌套对象中包含 List 子类类型的字段时，无法正确读取实际类型，比如目标类为：
// MultiPart -> Part -> HeaderManager -> JSONObject(should be Header)
// HttpSampler -> HttpRequest -> MultiPart -> JSONObject(should be Part)
//
// （2）在类上指定自定义的反序列化类
// 在 List 子类上指定自定义的反序列化类，不生效
// @JSONType(deserializer = MultiPart.MultiPartObjectReader.class)
// 跟踪源码发现，如果目标类为 List 子类，返回的是 com.alibaba.fastjson2.reader.ObjectReaderImplList 对象，而非自定义反序列化类对象，
// 如果目标类不是 List 子类，比如一个普通的 Bean 类，则 deserializer 会生效
//
// （3）手动注册自定义反序列化类，可以生效：
// JSON.register(MultiPart.class, new MultiPart.MultiPartObjectReader());
// 或在字段上指定自定义反序列化类，可以生效：
// @JSONField(name = "multipart", deserializeUsing = MultiPart.MultiPartObjectReader.class)
//
// 方案设计：这里直接使用 List<Part> 作为 MultiPart 成员变量，不就解决反序列化问题了吗？
// 是的，这样确实可以避免一些奇怪的问题，但 MultiPart 需要重写一些 List 的方法，以提供类似 List 的功能，
// 同时 Json 结构上多了一层（Json/Yaml 用例），Json 反序列化时需要去掉多的这一层，即：
// multiPart:                       (MultiPart)
//    parts:                        (List<Part> 成员变量)
//        - xxx                     (Part Json)
//        - xxx                     (Part Json)
// 综合考虑，这里直接通过 List 子类来实现更方便。
public class MultiPart
    extends ArrayList<Part>
    implements Copyable<MultiPart>, Mergeable<MultiPart>, Computable<MultiPart> {

    public static class MultiPartObjectReader implements ObjectReader<MultiPart> {

        @SuppressWarnings("rawtypes")
        @Override
        public MultiPart readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            List partsList = jsonReader.readArray();
            MultiPart multiPart = new MultiPart();
            for (Object partMap : partsList) {
                if (!(partMap instanceof Map)) {
                    throw new InvalidDataException("http.multipart[*] 数据结构非法，当前值：%s", JSON.toJSONString(partMap));
                }
                multiPart.add(JSON.parseObject(JSON.toJSONString(partMap), Part.class));
            }
            return multiPart;
        }
    }

    public MultiPart() {
    }

    public MultiPart(List<Part> parts) {
        addAll(parts);
    }

    @Override
    public MultiPart copy() {
        MultiPart res = new MultiPart();
        forEach(p -> res.add(p.copy()));
        return res;
    }

    @Override
    public MultiPart merge(MultiPart other) {
        return HttpModelSupport.multiValueManagerMerge(this, other, e -> {
            String name = e.getName();
            if (name == null) {
                name = getNameFromHeaders(e.getHeaders());
                if (name == null) {
                    name = UUID.randomUUID().toString();
                }
            }
            return name;
        });
    }

    // Content-Disposition: form-data; name="named"
    private String getNameFromHeaders(HeaderManager headerManager) {
        if (headerManager == null)
            return null;

        Header header = headerManager.getHeader("Content-Disposition");
        if (header == null)
            return null;

        String allValue = header.value;
        if (allValue == null)
            return null;

        String[] values = allValue.split(";");
        for (String value : values) {
            String[] nameAndValue = value.split("=");
            if (nameAndValue.length == 2 && "name".equalsIgnoreCase(nameAndValue[0])) {
                String s = nameAndValue[1].trim();
                if (s.length() > 2) {
                    return s.substring(1, s.length() - 1);
                }
            }
        }
        return null;
    }

    @Override
    public MultiPart eval(ContextWrapper ctx) {
        forEach(ctx::eval);
        return this;
    }

}
