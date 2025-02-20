package com.liyunx.groot.common;

/**
 * 表示一个类是可自查的类，即验证对象是否合法。
 *
 * <p>这样设计的目的是：
 * 1. 校验和执行逻辑分开，代码更清晰。
 * 2. 能单独校验而不执行，比如扫描所有待测用例，提前发现数据问题，防止执行到一半，用例因数据问题报错，提高执行成功率。
 * 3. 能提前校验发现问题，平台开发时，保证入库数据合法。
 * 4. 多次执行相同用例时，可以只校验一次，执行多次，节省时间。
 */
public interface Validatable {

    /**
     * 对象自查
     *
     * <p><br>约定：
     * <li>如果 valid 为 false，必须填入 reason（不能为 null 或空）。</li>
     * <li>reason 的填写格式："\n" + 数据非法原因。</li>
     *
     * @return 验证结果
     */
    //ValidateResult validate();
    default ValidateResult validate() {
        // nothing to do.
        return new ValidateResult();
    }

}
