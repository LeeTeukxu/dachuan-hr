package com.tianye.hrsystem.modules.salary.support;

import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.model.HrmEmployee;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TaxImportEmployeeMatcher {

    private TaxImportEmployeeMatcher() {
    }

    public static Long resolveEmployeeId(List<HrmEmployee> employees, String employeeName, String mobile, int displayRow) {
        String name = normalizeText(employeeName);
        String phone = normalizePhone(mobile);
        if (isEmpty(name)) {
            throw importError("第" + displayRow + "行员工姓名不能为空");
        }

        List<HrmEmployee> nameMatches = findByName(employees, name);
        if (!isEmpty(phone)) {
            List<HrmEmployee> exactMatches = new ArrayList<>();
            for (HrmEmployee employee : nameMatches) {
                if (phone.equals(normalizePhone(employee.getMobile()))) {
                    exactMatches.add(employee);
                }
            }
            if (exactMatches.size() == 1) {
                return requireEmployeeId(exactMatches.get(0), displayRow, name);
            }
            if (exactMatches.size() > 1) {
                throw importError("系统中存在重复员工：" + name + " + " + phone + "，请先处理后再导入");
            }
            throw importError("第" + displayRow + "行未找到员工：" + name + " + " + phone);
        }

        if (nameMatches.isEmpty()) {
            throw importError("第" + displayRow + "行未找到员工：" + name);
        }
        if (nameMatches.size() > 1) {
            throw importError("第" + displayRow + "行员工姓名重复：" + name + "，请在模板手机号列填写手机号");
        }
        return requireEmployeeId(nameMatches.get(0), displayRow, name);
    }

    public static void ensureUniqueEmployeePeriod(Set<String> importedKeys, Long employeeId, String employeeName, String period, int displayRow) {
        String key = employeeId + "|" + period;
        if (!importedKeys.add(key)) {
            throw importError("导入文件存在重复员工：" + employeeName + "，期间：" + period + "，第" + displayRow + "行重复");
        }
    }

    public static String readCellText(List<Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return "";
        }
        Object value = row.get(index);
        if (value == null) {
            return "";
        }
        return normalizeText(String.valueOf(value));
    }

    public static BigDecimal readBigDecimal(List<Object> row, int index) {
        String text = readCellText(row, index);
        if (isEmpty(text)) {
            return null;
        }
        return new BigDecimal(text.replace(",", ""));
    }

    public static Integer readInteger(List<Object> row, int index) {
        String text = readCellText(row, index);
        if (isEmpty(text)) {
            return null;
        }
        return new BigDecimal(text.replace(",", "")).intValue();
    }

    public static boolean isBlankRow(Collection<Object> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (Object value : row) {
            if (!isEmpty(value == null ? "" : String.valueOf(value))) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ').trim();
    }

    public static String normalizePhone(String value) {
        String phone = normalizeText(value).replace(" ", "").replace("-", "");
        if (phone.matches("\\d+\\.0+")) {
            phone = phone.substring(0, phone.indexOf('.'));
        } else if (phone.matches("[+-]?\\d+(\\.\\d+)?[eE][+-]?\\d+")) {
            phone = new BigDecimal(phone).toPlainString();
            if (phone.matches("\\d+\\.0+")) {
                phone = phone.substring(0, phone.indexOf('.'));
            }
        }
        return phone;
    }

    private static List<HrmEmployee> findByName(List<HrmEmployee> employees, String name) {
        List<HrmEmployee> result = new ArrayList<>();
        if (employees == null) {
            return result;
        }
        for (HrmEmployee employee : employees) {
            if (employee == null || Objects.equals(employee.getIsDel(), 1)) {
                continue;
            }
            if (name.equals(normalizeText(employee.getEmployeeName()))) {
                result.add(employee);
            }
        }
        return result;
    }

    private static Long requireEmployeeId(HrmEmployee employee, int displayRow, String employeeName) {
        if (employee.getEmployeeId() == null) {
            throw importError("第" + displayRow + "行员工ID为空：" + employeeName);
        }
        return employee.getEmployeeId();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static CrmException importError(String message) {
        return new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, message);
    }
}
