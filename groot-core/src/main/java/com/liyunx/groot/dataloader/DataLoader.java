package com.liyunx.groot.dataloader;

import com.liyunx.groot.Configuration;

/**
 * 用例数据加载接口，本类应该为线程安全类。
 *
 * <p>
 * 默认通过文件加载用例数据，可自定义 {@link Configuration} 设置其他数据加载形式。
 * 比如在测试平台开发时，实现自己的数据加载器，从数据库加载用例和 API 数据，从文件存储服务器加载用例中使用的文件数据。
 */
public interface DataLoader {

    /**
     * 责任链模式，设置下一个 DataLoader。当且仅当初始化时调用该方法。
     *
     * @param dataLoader 数据加载器
     * @return 方法参数值
     */
    DataLoader setNext(DataLoader dataLoader);

    /**
     * 根据文本内容加载
     *
     * @param text     文本内容
     * @param textType 文本类型，比如：json、yaml，统一全小写
     * @param clazz    目标 Class
     * @param <T>      目标 Class
     * @return 目标对象
     */
    <T> T load(String text, String textType, Class<T> clazz);

    /**
     * 根据标识符加载
     *
     * <p><br>
     * 标识符示例：<br>
     * - testcases/login.json            -> TestCase.class<br>
     * - testcases/login.yml             -> TestCase.class<br>
     * - testcases/data/data1.xlsx       -> File.class<br>
     * - F:\lab\wiremock\login_data.xlsx -> File.class<br>
     * 如果是平台开发，标识符可能是 ID，如用例 ID，API ID，文件 ID
     *
     * @param identifier 标识符
     * @param clazz      目标 Class
     * @param <T>        目标 Class
     * @return 目标对象
     */
    <T> T loadByID(String identifier, Class<T> clazz);

}
