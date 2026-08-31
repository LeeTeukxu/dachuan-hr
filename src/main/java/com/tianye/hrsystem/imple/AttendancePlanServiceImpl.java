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

    @Override
    public void Sync(String EmpIDS, Date Begin, Date End, List<tbattendanceuser> users)throws Exception {
        // 不在这里删除，由外层 HrmAttendanceDataServiceImpl 统一删除
        
        // 循环调用获取和保存（不执行删除）；users 由调用方逐层传参，消除单例共享可变字段
        List<Date> Dates=dateUtils.rangeDate(Begin,End);
        for(int i=0;i<Dates.size();i++){
            Date D=Dates.get(i);
            planRecord.GetAndSaveWithoutDelete(EmpIDS,D,users);
        }
        // 不能按“同名+无排班”删除映射，否则会误删重名员工（如不同部门同名）
        // 映射关系应保持稳定，由 userId/empId 唯一标识，不在同步计划阶段做破坏性清理
    }
}
