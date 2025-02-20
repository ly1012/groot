package com.liyunx.groot.functions;

import com.liyunx.groot.context.ContextWrapper;
import jdk.jfr.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 打印 Info 级别日志，参数为任意类型对象
 * <p>
 * 打印参数信息，支持多个参数
 */
@Experimental
public class PrintFunction extends AbstractFunction {

    private static final Logger log = LoggerFactory.getLogger(PrintFunction.class);

    @Override
    public String getName() {
        return "print";
    }

    @Override
    public Object execute(ContextWrapper contextWrapper, List<Object> parameters) {
        for (Object var : parameters) {
            log.info("type：{}, value：{}", var != null ? var.getClass().getSimpleName() : "null", var);
        }
        return "";
    }

}
