package com.liyunx.groot.processor;

import com.liyunx.groot.builder.TestBuilder;

/**
 * 通用字段和逻辑处理
 *
 * <p>实现类尽量继承该类
 */
public abstract class AbstractProcessor implements Processor {

    protected boolean disabled;
    protected String name;
    protected String description;

    public AbstractProcessor() {
    }

    protected AbstractProcessor(Builder<?, ?> builder) {
        this.disabled = builder.disabled;
        this.name = builder.name;
        this.description = builder.description;
    }

    @Override
    public boolean disabled() {
        return disabled;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static abstract class Builder<T, SELF extends Builder<T, SELF>>
        implements TestBuilder<T> {

        protected SELF self;

        private boolean disabled;
        private String name;
        private String description;

        @SuppressWarnings("unchecked")
        protected Builder() {
            self = (SELF) this;
        }

        public SELF disable(boolean disabled) {
            this.disabled = disabled;
            return self;
        }

        public SELF name(String name) {
            this.name = name;
            return self;
        }

        public SELF description(String description) {
            this.description = description;
            return self;
        }



    }

}
