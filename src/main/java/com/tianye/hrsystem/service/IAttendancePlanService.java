package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: IAttendancePlanService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 15:58
 **/
public interface IAttendancePlanService {
    void setUsers(List<tbattendanceuser> users);
    void Sync(String EmpIDS, Date Begin, Date End) throws Exception;
}
