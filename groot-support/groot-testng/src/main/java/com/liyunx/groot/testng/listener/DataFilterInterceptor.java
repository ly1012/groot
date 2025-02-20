package com.liyunx.groot.testng.listener;

import com.liyunx.groot.testelement.ParametersData;
import com.liyunx.groot.testng.annotation.DataFilter;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.testng.IDataProviderInterceptor;
import org.testng.IDataProviderMethod;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.testng.support.AnnotationSupport.getDataFilter;

public class DataFilterInterceptor implements IDataProviderInterceptor {

    @Override
    public Iterator<Object[]> intercept(Iterator<Object[]> original,
                                        IDataProviderMethod dataProviderMethod,
                                        ITestNGMethod method,
                                        ITestContext iTestContext) {
        DataFilter annotation = getDataFilter(method.getConstructorOrMethod().getMethod());
        if (annotation == null) {
            return original;
        }

        String slice = annotation.slice();
        String expr = annotation.expr();
        if ((slice == null || slice.trim().isEmpty()) &&
            (expr == null || expr.trim().isEmpty())) {
            return original;
        }

        // 获取所有数据
        List<Object[]> dataList = new ArrayList<>();
        while (original.hasNext()) {
            Object[] data = original.next();
            dataList.add(data);
        }

        // 过滤数据
        List<Object[]> result = new ArrayList<>();
        List<Integer> parsedSeq = ParametersData.parseSeq(slice, dataList.size());
        Script script = null;
        if (expr != null && !expr.trim().isEmpty()) {
            script = LazyHolder.GroovyShellInstance.parse(expr);
        }
        if (script == null) {
            for (Integer index : parsedSeq) {
                result.add(dataList.get(index - 1));
            }
        } else {
            for (Integer index : parsedSeq) {
                Object[] data = dataList.get(index - 1);
                script.setBinding(getBinding(data));
                Object scriptResult = script.run();
                if (scriptResult != null && "true".equalsIgnoreCase(String.valueOf(scriptResult))) {
                    result.add(data);
                }
            }
        }

        return result.iterator();
    }

    private Binding getBinding(Object[] data) {
        Binding binding = new Binding();
        if (data.length == 1 && data[0] instanceof Map) {
            //noinspection unchecked
            ((Map<String, ?>) data[0]).forEach(binding::setVariable);
        } else {
            binding.setVariable("data", data);
            for (int i = 0; i < data.length; i++) {
                binding.setVariable("p" + (i + 1), data[i]);
            }
        }
        return binding;
    }

    static class LazyHolder {
        static final GroovyShell GroovyShellInstance = new GroovyShell();
    }

}
