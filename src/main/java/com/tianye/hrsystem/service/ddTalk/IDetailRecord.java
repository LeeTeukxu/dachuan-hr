package com.tianye.hrsystem.service.ddTalk;

import com.taobao.api.ApiException;
import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: IDetailRecord
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月18日 11:25
 **/
public interface IDetailRecord {
    void GetAndSave(Date Begin,Date End) throws ApiException;
    void GetAndSave(String EmpID,Date Begin, Date End) throws ApiException;
    void setUsers(List<tbattendanceuser> users);
}
