package com.tianye.hrsystem.common;


import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.AdminRole;
import com.tianye.hrsystem.entity.vo.EmployeeInfo;
import com.tianye.hrsystem.model.LoginUserInfo;

public class EmployeeHolder {

    private static ThreadLocal<EmployeeInfo> employeeInfoThreadLocal = new ThreadLocal<>();


    public static EmployeeInfo getEmployeeInfo() {
        return employeeInfoThreadLocal.get();
    }

    public static Long getEmployeeId() {
        if (employeeInfoThreadLocal.get() != null) {
            return employeeInfoThreadLocal.get().getEmployeeId();
        } else {
            return -1L;
        }
    }

    /**
     * 是否是人资管理角色
     *
     * @return
     */
    public static boolean isHrmAdmin() {
        LoginUserInfo Info= CompanyContext.get();
        return Info.getRoleName().equals("系统管理员");
//        EmployeeInfo employeeInfo = employeeInfoThreadLocal.get();
//        AdminRole role = employeeInfo.getRole();
//        return UserUtil.isAdmin() || (role != null && role.getLabel() == 91);
    }

    public static void setEmployeeInfo(EmployeeInfo employeeInfo) {
        employeeInfoThreadLocal.set(employeeInfo);
    }

    public static void remove() {
        employeeInfoThreadLocal.remove();
    }
}

