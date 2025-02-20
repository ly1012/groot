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

/**
 * form
 */
public class FormParamManager
    extends ArrayList<FormParam>
    implements Copyable<FormParamManager>, Mergeable<FormParamManager>, Computable<FormParamManager> {

    public static class FormParamManagerObjectReader implements ObjectReader<FormParamManager> {

        @SuppressWarnings("rawtypes")
        @Override
        public FormParamManager readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            List partsList = jsonReader.readArray();
            FormParamManager formParams = new FormParamManager();
            for (Object partMap : partsList) {
                if (!(partMap instanceof Map)) {
                    throw new InvalidDataException("http.form 数据结构非法，当前值：%s", JSON.toJSONString(partMap));
                }
                formParams.add(JSON.parseObject(JSON.toJSONString(partMap), FormParam.class));
            }
            return formParams;
        }
    }

    public FormParamManager() {
    }

    public FormParamManager(List<FormParam> formParams) {
        addAll(formParams);
    }

    @Override
    public FormParamManager copy() {
        FormParamManager res = new FormParamManager();
        forEach(p -> res.add(p.copy()));
        return res;
    }

    @Override
    public FormParamManager merge(FormParamManager other) {
        return HttpModelSupport.multiValueManagerMerge(this, other, e -> e.name);
        //if (other == null){
        //  return copy();
        //}
        //// this     other     merged
        ////          a: ao1    a: ao1
        ////          a: ao2    a: ao2
        //// b: bt1   b: bo     b: bo
        //// b: bt2
        //// c: ct1   c: co1    c: co1
        ////          c: co2    c: co2
        //// d: dt              d: dt
        //// 观察上面的合并策略，可以看出当以 other 为基准进行合并时，
        //// 只需要将 this 中有而 other 中没有的 name 加入 other 即可完成合并。
        //FormParamManager res = other.copy();
        //for (FormParam param : this) {
        //  // 查询当前 name 是否存在于 other 中
        //  String name = param.name;
        //  boolean found = false;
        //  for (FormParam otherParam : other) {
        //    if (name.equalsIgnoreCase(otherParam.name)){
        //      found = true;
        //      break;
        //    }
        //  }
        //  // 如果当前 name 在 other 中不存在，则加入合并数据
        //  if (!found){
        //    res.add(param);
        //  }
        //}
        //return res;
    }

    @Override
    public FormParamManager eval(ContextWrapper ctx) {
        forEach(ctx::eval);
        return this;
    }

}
