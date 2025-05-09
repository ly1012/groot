package com.liyunx.groot;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.functions.AbstractFunction;

import java.util.List;

public class GrootTestNGTestCase extends AbstractGrootTestNGTestCase {

    private static final Groot groot;

    static {
        groot = new Groot();
        ApplicationConfig.getFunctions().add(new AbstractFunction() {
            @Override
            public String getName() {
                return "print";
            }

            @Override
            public Object execute(ContextWrapper contextWrapper, List<Object> parameters) {
                for (Object var : parameters) {
                    System.out.println(var);
                }
                return "";
            }
        });
    }

    @Override
    protected Groot getGroot() {
        return groot;
    }

}
