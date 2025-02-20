package com.liyunx.groot.common;

/**
 * 唯一性接口，如果一个类实现了该接口，表示具备相同 uniqueId 的多个类只有一个会被使用（唯一选取策略取决于业务逻辑）。
 */
public interface Unique {

    String uniqueId();

}
