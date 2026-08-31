package com.tianye.hrsystem.util;

/**
 * SaaS 安全改造 P0-4：SQL 标识符/排序参数白名单校验工具
 * 用于过滤动态拼入 SQL 的表名、列名、排序字段等无法参数化的部分
 */
public final class SqlSafeUtil {

    private static final String IDENTIFIER_PATTERN = "^[A-Za-z0-9_]{1,64}$";
    /** 允许 列名.列名 形式（如 a.sortField）*/
    private static final String QUALIFIED_IDENTIFIER_PATTERN = "^[A-Za-z0-9_]{1,64}(\\.[A-Za-z0-9_]{1,64})?$";

    private SqlSafeUtil() {
    }

    /**
     * 校验并返回安全的标识符（表名/列名，允许 a.b 形式），不合法则抛出异常
     */
    public static String safeIdentifier(String identifier) {
        if (identifier == null || !identifier.matches(QUALIFIED_IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException("非法的排序/标识符参数: " + identifier);
        }
        return identifier;
    }

    /**
     * 供 MyBatis &lt;bind&gt; 使用的空安全版本：null/空串返回空串（配合 &lt;when test&gt; 守卫使用），
     * 非法字符仍然抛出异常以拦截注入
     */
    public static String safeIdentifierOrEmpty(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return "";
        }
        return safeIdentifier(identifier);
    }

    /**
     * 校验排序方向，只允许 asc/desc（忽略大小写），不合法则默认 asc
     */
    public static String safeSortOrder(String sortOrder) {
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return "desc";
        }
        return "asc";
    }

    /**
     * 分页起始位置校验：非负整数，不合法返回 0
     */
    public static Integer safePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 0) {
            return 0;
        }
        return Math.min(pageNum, 100000000);
    }

    /**
     * 分页大小校验：1~5000，不合法返回 10
     */
    public static Integer safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 5000);
    }
}
