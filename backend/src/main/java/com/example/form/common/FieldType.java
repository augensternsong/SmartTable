package com.example.form.common;

/**
 * 栏位类型枚举.
 *
 * <p>前端据此选择不同的输入组件; 后端据此做值校验和序列化.
 */
public enum FieldType {
    /** 短文本 */
    TEXT,
    /** 长文本 */
    TEXTAREA,
    /** 数字 */
    NUMBER,
    /** 日期 yyyy-MM-dd */
    DATE,
    /** 日期时间 yyyy-MM-dd HH:mm:ss */
    DATETIME,
    /** 单选下拉 */
    SELECT_SINGLE,
    /** 多选下拉(存 JSON 数组字符串) */
    SELECT_MULTI;

    public boolean isSelectType() {
        return this == SELECT_SINGLE || this == SELECT_MULTI;
    }

    public boolean isMultiSelect() {
        return this == SELECT_MULTI;
    }
}
