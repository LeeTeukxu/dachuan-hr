package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.HrmEmployeeEmploymentRecord;
import com.tianye.hrsystem.enums.EmploymentRecordType;
import com.tianye.hrsystem.mapper.HrmEmployeeEmploymentRecordMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeEmploymentRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工入离职履历 服务实现类
 *
 * @since 2026-08-26
 */
@Service
public class HrmEmployeeEmploymentRecordServiceImpl extends BaseServiceImpl<HrmEmployeeEmploymentRecordMapper, HrmEmployeeEmploymentRecord> implements IHrmEmployeeEmploymentRecordService {

    @Override
    public void recordEntry(Long employeeId, LocalDate entryTime, boolean againEntry) {
        if (employeeId == null) {
            return;
        }
        HrmEmployeeEmploymentRecord record = new HrmEmployeeEmploymentRecord();
        record.setEmployeeId(employeeId);
        record.setType(againEntry ? EmploymentRecordType.AGAIN_ENTRY.getValue() : EmploymentRecordType.ENTRY.getValue());
        record.setNodeTime(entryTime);
        save(record);
    }

    @Override
    public void recordLeave(Long employeeId, LocalDate leaveTime, String remarks) {
        if (employeeId == null || leaveTime == null) {
            return;
        }
        // 幂等：办理离职可能因修改离职信息被再次触发，只保留最新一条未归档离职节点
        lambdaUpdate()
                .eq(HrmEmployeeEmploymentRecord::getEmployeeId, employeeId)
                .eq(HrmEmployeeEmploymentRecord::getType, EmploymentRecordType.LEAVE.getValue())
                .remove();
        HrmEmployeeEmploymentRecord record = new HrmEmployeeEmploymentRecord();
        record.setEmployeeId(employeeId);
        record.setType(EmploymentRecordType.LEAVE.getValue());
        record.setNodeTime(leaveTime);
        record.setRemarks(remarks);
        save(record);
    }

    @Override
    public void cancelLeave(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        lambdaUpdate()
                .eq(HrmEmployeeEmploymentRecord::getEmployeeId, employeeId)
                .eq(HrmEmployeeEmploymentRecord::getType, EmploymentRecordType.LEAVE.getValue())
                .remove();
    }

    @Override
    public List<HrmEmployeeEmploymentRecord> queryByEmployeeId(Long employeeId) {
        return lambdaQuery()
                .eq(HrmEmployeeEmploymentRecord::getEmployeeId, employeeId)
                .orderByDesc(HrmEmployeeEmploymentRecord::getCreateTime)
                .orderByDesc(HrmEmployeeEmploymentRecord::getRecordId)
                .list();
    }
}
