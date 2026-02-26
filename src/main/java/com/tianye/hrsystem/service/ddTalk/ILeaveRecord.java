package com.tianye.hrsystem.service.ddTalk;

import com.taobao.api.ApiException;
import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: ILeaveRecord
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月11日 15:52
 **/
public interface ILeaveRecord {
    void GetAndSave(Date WorkDate) throws ApiException;
    void setUsers(List<tbattendanceuser> users);
}
