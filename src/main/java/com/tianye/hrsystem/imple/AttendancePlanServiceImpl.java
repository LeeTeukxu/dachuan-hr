package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.imple.ddTalk.AttendancePlanRecord;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.service.IAttendancePlanService;
import com.tianye.hrsystem.util.MyDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: AttendancePlanServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 16:04
 **/

@Service
public class AttendancePlanServiceImpl implements IAttendancePlanService {
    @Autowired
    AttendancePlanRecord planRecord;
    @Autowired
    MyDateUtils dateUtils;

    List<tbattendanceuser> users;
    @Override
    public void setUsers(List<tbattendanceuser> users) {
        this.users=users;
    }

    @Override
    public void Sync(String EmpIDS, Date Begin, Date End)throws Exception {
        // 不在这里删除，由外层 HrmAttendanceDataServiceImpl 统一删除
        
        // 循环调用获取和保存（不执行删除）
        List<Date> Dates=dateUtils.rangeDate(Begin,End);
        for(int i=0;i<Dates.size();i++){
            Date D=Dates.get(i);
            planRecord.setUsers(users);
            planRecord.GetAndSaveWithoutDelete(EmpIDS,D);
        }
        planRecord.DeleteRepeatUser(Begin,End);
    }
}
