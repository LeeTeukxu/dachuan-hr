package com.tianye.hrsystem.service.employee;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmEmployeeAbnormalChangeRecord;
import com.tianye.hrsystem.enums.AbnormalChangeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: IHrmEmployeeAbnormalChangeRecordService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月27日 21:38
 **/
public interface IHrmEmployeeAbnormalChangeRecordService extends BaseService<HrmEmployeeAbnormalChangeRecord> {


    /**
     * 添加异动记录
     *
     * @param employeeId         员工id
     * @param abnormalChangeType 异动类型
     */
    void addAbnormalChangeRecord(Long employeeId, AbnormalChangeType abnormalChangeType, LocalDateTime changeTime);

    /**
     * 按年和月查询员工异动记录
     *
     * @return
     */
    List<HrmEmployeeAbnormalChangeRecord> queryListByDate(LocalDate startTime, LocalDate endTime, Collection<Long> employeeIds, Integer type);

    /**
     * 按年和月查询员工异动记录
     *
     * @return
     */
    List<HrmEmployeeAbnormalChangeRecord> queryListByDate1(int year, int monthValue, Integer type, Collection<Long> employeeIds);
}
