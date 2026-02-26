package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.ddAccount;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface ddAccountMapper extends BaseMapper<ddAccount> {
    @Select(value = "Select * from ${database}.ddAccount")
    List<ddAccount> getAllAccount(String database);

    /**
     * create by: mmzs
     * description: TODO
     * create time:
     *
     报表数据已保存到的最大日期值。
     * @return
     */
    @Select(value="Select Max(Work_Date) from hrm_attendance_report_data")
    Date getMaxReportDate();
    @Select(value="Select Max(Work_Date) from hrm_attendance_report_data")
    Date getMinReportDate();
    /**
     * create by: mmzs
     * description: TODO
     * create time:
     *
     加班和请假记录保存的最大日期
     * @return
     */
    @Select(value="Select Max(RDate) from (Select Max(Create_Time) as RDate from hrm_employee_leave_record UNION Select Max(Create_Time) as RDate from hrm_employee_over_time_record)t")
    Date getMaxSavedData();
}
