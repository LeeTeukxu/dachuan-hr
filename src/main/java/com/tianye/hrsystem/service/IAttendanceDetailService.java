package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: IAttendanceDetailService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 15:55
 **/
public interface IAttendanceDetailService {
    void Sync(String EmpIDS, Date Begin, Date End, List<tbattendanceuser> users) throws Exception;
}
