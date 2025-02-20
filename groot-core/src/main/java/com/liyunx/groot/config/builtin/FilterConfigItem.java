package com.liyunx.groot.config.builtin;

import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.filter.TestFilter;

import java.util.ArrayList;

/**
 * Filter 配置上下文
 */
@KeyWord(FilterConfigItem.KEY)
public class FilterConfigItem extends ArrayList<TestFilter> implements ConfigItem<FilterConfigItem> {

    public static final String KEY = "filters";

    @Override
    public FilterConfigItem merge(FilterConfigItem other) {
        FilterConfigItem filterConfigItem = new FilterConfigItem();
        filterConfigItem.addAll(this);
        if (other != null)
            filterConfigItem.addAll(other);
        return filterConfigItem;
    }

    /**
     * 插件配置 Builder
     */
    public static class Builder {

        FilterConfigItem filterConfigItem = new FilterConfigItem();

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder apply(TestFilter filter) {
            filterConfigItem.add(filter);
            return this;
        }

        public FilterConfigItem build() {
            return filterConfigItem;
        }

    }

}
