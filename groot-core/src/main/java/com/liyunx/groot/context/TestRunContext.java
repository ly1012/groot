package com.liyunx.groot.context;

import com.liyunx.groot.config.ConfigGroup;

/**
 * 测试上下文抽象类，提供基础实现
 */
public abstract class TestRunContext implements Context {

    protected ConfigGroup configGroup;
    protected int invert = 0;

    // == Getter/Setter ==

    @Override
    public ConfigGroup getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(ConfigGroup configGroup) {
        this.configGroup = configGroup;
    }

    @Override
    public int getInvert() {
        return invert;
    }

    public void setInvert(int invert) {
        this.invert = invert;
    }

    /**
     * 暂时用不到
     */
    //@Deprecated
    //protected TestElement testElement;

    //public static TestRunContext emptyContext(){
    //  TestRunContext context = new TestRunContext();
    //  context.setVariables(new HashMap<>());
    //  context.setProperties(new Properties());
    //  return context;
    //}

}
