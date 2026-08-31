package com.tianye.hrsystem.service.employee;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmEmployeeEmploymentRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工入离职履历 服务接口
 *
 * @since 2026-08-26
 */
public interface IHrmEmployeeEmploymentRecordService extends BaseService<HrmEmployeeEmploymentRecord> {

    /**
     * 记录入职/再入职时间节点
     *
     * @param employeeId 员工id
     * @param entryTime  入职日期
     * @param againEntry 是否再入职
     */
    void recordEntry(Long employeeId, LocalDate entryTime, boolean againEntry);

    /**
     * 记录离职时间节点（幂等：同一员工未归档离职记录仅保留最新一条）
     *
     * @param employeeId 员工id
     * @param leaveTime  离职日期（计划离职日期口径）
     * @param remarks    备注
     */
    void recordLeave(Long employeeId, LocalDate leaveTime, String remarks);

    /**
     * 取消离职时移除最近一条离职节点
     *
     * @param employeeId 员工id
     */
    void cancelLeave(Long employeeId);

    /**
     * 查询员工入离职履历（按记录时间倒序）
     *
     * @param employeeId 员工id
     */
    List<HrmEmployeeEmploymentRecord> queryByEmployeeId(Long employeeId);
}
