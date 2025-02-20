package com.liyunx.groot.dataloader.file;

import java.io.File;

/**
 * 根据标识符加载为 {@link File}
 */
public class FileDataLoader extends LocalDataLoader {

    @Override
    protected <T> T next(String text, String textType, Class<T> clazz) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T nextByID(String identifier, Class<T> clazz) {
        if (File.class.equals(clazz)) {
            return (T) getAbsolutePath(identifier, null).toFile();
        }
        return null;
    }

}
