package com.liyunx.groot.template.freemarker;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.functions.Function;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;

import java.util.ArrayList;
import java.util.List;

/**
 * FreeMarker 函数适配器，将 FreeMarker 函数转为 Groot 函数
 */
public class FreeMarkerFunctionAdapter implements TemplateMethodModelEx {

    private final ContextWrapper contextWrapper;
    private final Function function;

    public FreeMarkerFunctionAdapter(ContextWrapper contextWrapper, Function function) {
        this.contextWrapper = contextWrapper;
        this.function = function;
    }

    public Object exec(List args) {
        // 将 TemplateModel 包装类对象解包为模板中传递的原始 Object 对象
        List<Object> list = new ArrayList<>();
        args.forEach(e -> {
            try {
                list.add(DeepUnwrap.unwrap((TemplateModel) e));
            } catch (TemplateModelException templateModelException) {
                templateModelException.printStackTrace();
            }
        });
        // 调用 Function
        return function.execute(contextWrapper, list);
    }

}
