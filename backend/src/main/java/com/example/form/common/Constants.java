package com.example.form.common;

/**
 * 系统级常量.
 */
public final class Constants {

    private Constants() {
    }

    /** 角色编码 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    /** Redis Key 前缀 */
    public static final String CACHE_PREFIX = "form:cache:";
    public static final String CACHE_USER_PERM = CACHE_PREFIX + "user:perm:";
    public static final String CACHE_USER_INFO = CACHE_PREFIX + "user:info:";
    public static final String CACHE_TEMPLATE = CACHE_PREFIX + "template:";
    public static final String CACHE_TEMPLATE_FIELDS = CACHE_PREFIX + "template:fields:";

    public static final String JWT_BLACKLIST = "form:jwt:blacklist:";

    /** 表单状态 */
    public static final String TEMPLATE_DRAFT = "DRAFT";
    public static final String TEMPLATE_PUBLISHED = "PUBLISHED";
    public static final String TEMPLATE_ARCHIVED = "ARCHIVED";

    /** 栏位状态 */
    public static final String FIELD_ACTIVE = "ACTIVE";
    public static final String FIELD_INACTIVE = "INACTIVE";

    /** 启用状态 */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}
