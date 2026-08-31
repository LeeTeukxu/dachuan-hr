package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Date;
import java.util.List;

public interface IHolidayDataService {

    void Sync(String EmpIDS, Date Begin, Date End, List<tbattendanceuser> users) throws Exception;
}
