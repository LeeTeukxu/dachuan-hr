package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

public interface IHolidayDataService {

    void setUsers(List<tbattendanceuser> users);
    void Sync(String EmpIDS, Date Begin, Date End) throws Exception;
}
