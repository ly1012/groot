package com.liyunx.groot.testng.support;

import com.liyunx.groot.TestRunner;
import org.testng.IDataProviderMethod;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 静态代理类，用于存储 TestRunner 对象
 */
public class DataProviderMethodProxy implements IDataProviderMethod {

    private final IDataProviderMethod target;
    private final TestRunner testRunner;

    public DataProviderMethodProxy(IDataProviderMethod dataProviderMethod, TestRunner testRunner) {
        this.target = dataProviderMethod;
        this.testRunner = testRunner;
    }

    public TestRunner getTestRunner() {
        return testRunner;
    }

    @Override
    public Object getInstance() {
        return target.getInstance();
    }

    @Override
    public Method getMethod() {
        return target.getMethod();
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public boolean isParallel() {
        return target.isParallel();
    }

    @Override
    public List<Integer> getIndices() {
        return target.getIndices();
    }

}
