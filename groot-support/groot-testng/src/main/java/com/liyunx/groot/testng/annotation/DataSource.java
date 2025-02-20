package com.liyunx.groot.testng.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataSource {

    /**
     * 文件数据源
     *
     * <p>当前支持文件类型：JSON、Yaml(yml 或 yaml)、CSV、Excel(xls 或 xlsx)</p>
     *
     * <ul>
     *     <li>{@code CSV: data.csv?format=groot}</li>
     *     <li>{@code CSV: data.csv?delimiter=,&escape=\&quote="}</li>
     *     <li>{@code Excel(SheetName): data.xls?name=sheet1}</li>
     *     <li>{@code Excel(SheetIndex 1-based): data.xls?index=2}</li>
     * </ul>
     *
     * @return 文件路径，默认根目录为当前项目的 src/test/resources
     */
    String value() default "";

    /**
     * 暂不支持
     *
     * <p>数据源如果是 DB，如何配置？ID 引用数据库配置或数据库连接池？</p>
     *
     * @return SQL 语句
     */
    String sql() default "";

    /**
     * 是否并行执行
     *
     * @return true: 并行执行
     */
    boolean parallel() default false;

}
