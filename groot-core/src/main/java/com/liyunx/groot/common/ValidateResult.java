package com.liyunx.groot.common;

/**
 * {@link Validatable#validate()} 验证结果实体类
 * <p>只有两种情况，最终验证结果为成功（true），即当且仅当所有验证结果为 true，最终验证结果才为 true：
 * <ul>
 *     <li>new ValidateResult()</li>
 *     <li>append(success) {0, n}</li>
 * </ul>
 */
public class ValidateResult {

    private boolean valid = true;
    private final StringBuilder reason = new StringBuilder();

    /**
     * 仅追加描述信息，不改变验证结果
     *
     * @param description 描述信息
     * @return 当前对象
     */
    public ValidateResult appendDescription(String description) {
        if (description == null) return this;
        reason.append(description);
        return this;
    }

    /**
     * 追加指定 Validatable 的验证结果
     *
     * @param validatable 要验证的对象
     */
    public ValidateResult append(Validatable validatable) {
        if (validatable == null) return this;

        ValidateResult r = validatable.validate();
        // 任意一个失败，即为失败
        if (!r.isValid()) {
            valid = false;
            reason.append(r.getReason());
        }

        return this;
    }

    /**
     * 追加失败原因，并暗示验证失败
     *
     * @param extraReason 额外的失败原因
     * @return 当前对象
     * @see #appendDescription(String)
     */
    public ValidateResult append(String extraReason) {
        if (extraReason == null) return this;

        valid = false;
        reason.append(extraReason);

        return this;
    }

    /**
     * 追加失败原因，并暗示验证失败
     *
     * @param extraReasonTemplate 额外的失败原因（模板，基于 String.format）
     * @param args                模板参数
     * @return 更新后的验证结果对象
     * @see #appendDescription(String)
     */
    public ValidateResult append(String extraReasonTemplate, Object... args) {
        if (extraReasonTemplate == null) return this;

        return append(String.format(extraReasonTemplate, args));
    }

    // ---------------------------------------------------------------------
    // Getter/Setter
    // ---------------------------------------------------------------------

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason.toString();
    }

}
