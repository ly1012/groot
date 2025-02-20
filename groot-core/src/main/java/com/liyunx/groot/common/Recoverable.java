package com.liyunx.groot.common;

import com.liyunx.groot.SessionRunner;

/**
 * 表示类对象可恢复到初始状态。
 * <p/>
 * 实现类应当自己保存初始数据。
 */
public interface Recoverable {

    /**
     * 恢复当前对象到初始状态
     */
    void recover(SessionRunner session);

}
