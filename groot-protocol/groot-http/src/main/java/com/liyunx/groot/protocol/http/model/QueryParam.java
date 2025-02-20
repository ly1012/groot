package com.liyunx.groot.protocol.http.model;

import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.context.ContextWrapper;

/**
 * Url 查询参数，允许重复 Key（即多值），如 ?id=89&id=73&code=1
 */
public class QueryParam extends NameValue<String> implements Copyable<QueryParam>, Computable<QueryParam> {

    public QueryParam() {
    }

    public QueryParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public QueryParam copy() {
        QueryParam res = new QueryParam();
        res.name = name;
        res.value = value;
        return res;
    }

    @Override
    public QueryParam eval(ContextWrapper ctx) {
        name = ctx.evalAsString(name);

        if (value == null) {
            value = "";
        } else {
            value = ctx.evalAsString(value);
        }
        return this;
    }

    @Override
    public String toString() {
        return "QueryParam{" +
            "name='" + name + '\'' +
            ", value=" + value +
            '}';
    }

}
