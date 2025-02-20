package com.liyunx.groot.config.builtin;

import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.config.ConfigItem;
import com.liyunx.groot.processor.extractor.ExtractScope;

@KeyWord(ExtractConfigItem.KEY)
public class ExtractConfigItem implements ConfigItem<ExtractConfigItem> {

    public static final String KEY = "extract";

    @JSONField(name = "scope")
    private ExtractScope scope;

    //private boolean throwException;

    @Override
    public ExtractConfigItem copy() {
        ExtractConfigItem res = new ExtractConfigItem();
        res.scope = scope;
        return res;
    }

    @Override
    public ExtractConfigItem merge(ExtractConfigItem other) {
        ExtractConfigItem res = copy();
        if (other == null) return res;

        if (other.scope != null)
            res.scope = other.scope;

        return res;
    }

    public ExtractScope getScope() {
        return scope;
    }

    public void setScope(ExtractScope scope) {
        this.scope = scope;
    }
}
