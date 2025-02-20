package com.liyunx.groot.protocol.http.model;

import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.context.ContextWrapper;

/**
 * form
 */
public class FormParam extends NameValue<String> implements Copyable<FormParam>, Computable<FormParam> {

    public FormParam() {
    }

    public FormParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public FormParam copy() {
        FormParam res = new FormParam();
        res.name = name;
        res.value = value;
        return res;
    }

    @Override
    public FormParam eval(ContextWrapper ctx) {
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
        return "FormParam{" +
            "name='" + name + '\'' +
            ", value=" + value +
            '}';
    }
}
