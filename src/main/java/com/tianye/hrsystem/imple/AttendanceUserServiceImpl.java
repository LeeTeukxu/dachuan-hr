package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IAttendanceUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: AttendanceUserServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月17日 10:34
 **/
@Service
public class AttendanceUserServiceImpl implements IAttendanceUserService {

    @Autowired
    tbattendanceuserRepository userRep;
    @Override
    @Cacheable()
    public List<tbattendanceuser> getAll() {
        return null;
    }
}
