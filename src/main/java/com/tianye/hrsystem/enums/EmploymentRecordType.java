package com.tianye.hrsystem.enums;

import lombok.Getter;

/**
 * 员工入离职履历类型枚举
 */
@Getter
public enum EmploymentRecordType {
    ENTRY(1, "入职"),
    AGAIN_ENTRY(2, "再入职"),
    LEAVE(3, "离职");

    EmploymentRecordType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    private final String name;
    private final int value;

    public static String parseName(Integer value) {
        if (value == null) {
            return "";
        }
        for (EmploymentRecordType type : EmploymentRecordType.values()) {
            if (type.value == value) {
                return type.name;
            }
        }
        return "";
    }
}
