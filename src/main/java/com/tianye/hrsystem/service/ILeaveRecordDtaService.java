package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: ILeaveRecordService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:01
 **/
public interface ILeaveRecordDtaService {

    void Sync(String EmpIDS, Date Begin, Date End, List<tbattendanceuser> users) throws Exception;
}
