package com.liyunx.groot.dataloader;

import com.liyunx.groot.util.StringUtil;

/**
 * 责任链模式通用处理，子类重写 next 和 nextByID 方法，只需关注加载逻辑。
 *
 * <p>如果一个 DataLoader 实现类无法处理请求，应当返回 null，表示自己无法处理该请求。
 */
public abstract class AbstractDataLoader implements DataLoader {

    private DataLoader next;

    @Override
    public DataLoader setNext(DataLoader next) {
        this.next = next;
        return next;
    }

    @Override
    public <T> T load(String text, String textType, Class<T> clazz) {
        textType = textType.trim().toLowerCase();
        T res = next(text, textType, clazz);

        if (res != null)
            return res;

        if (next != null)
            return next.load(text, textType, clazz);

        throw new DataLoadException(
            "不支持的加载参数。Class: %s，textType：%s。",
            clazz.getCanonicalName(),
            textType);
    }

    @Override
    public <T> T loadByID(String identifier, Class<T> clazz) {
        T res;

        try {
            res = nextByID(identifier, clazz);
        } catch (Exception e) {
            throw new DataLoadException("%s 加载失败", e, identifier);
        }

        if (res != null)
            return res;

        if (next != null)
            return next.loadByID(identifier, clazz);

        throw new DataLoadException(
            "不支持的加载参数。Class：%s，identifier：%s。",
            clazz.getCanonicalName(),
            identifier);
    }

    protected abstract <T> T next(String text, String textType, Class<T> clazz);

    protected abstract <T> T nextByID(String identifier, Class<T> clazz);

    protected <T> void requireNotEmpty(String text, String textType, Class<T> clazz) {
        if (!StringUtil.hasValue(text))
            throw new DataLoadException("加载失败：文本内容为空。文本类型：%s，目标类：%s", null, textType, clazz.getName());
    }

}
