package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryLeaveRecordPageListBO;
import com.tianye.hrsystem.entity.bo.SetEmployeeLeaveRecordBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceExamine;
import com.tianye.hrsystem.entity.po.HrmEmployeeLeaveRecord;
import com.tianye.hrsystem.mapper.HrmEmployeeLeaveRecordMapper;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.service.IHrmAttendanceExamineService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeLeaveRecordService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 员工请假记录 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-13
 */
@Service
public class HrmEmployeeLeaveRecordServiceImpl extends BaseServiceImpl<HrmEmployeeLeaveRecordMapper, HrmEmployeeLeaveRecord> implements IHrmEmployeeLeaveRecordService {

    @Autowired
    private HrmEmployeeLeaveRecordMapper leaveRecordMapper;

    @Autowired
    private IHrmAttendanceExamineService attendanceExamineService;

    @Autowired
    private IHrmEmployeeService employeeService;

    @Override
    public BasePage<Map<String, Object>> queryLeaveRecordPageList(QueryLeaveRecordPageListBO leaveRecordPageListBO) {
        queryOaLeaveExamineList();
        String sortField = StrUtil.isNotEmpty(leaveRecordPageListBO.getSortField()) ? StrUtil.toUnderlineCase(leaveRecordPageListBO.getSortField()) : null;
        leaveRecordPageListBO.setSortField(sortField);
        BasePage<Map<String, Object>> page = leaveRecordMapper.queryLeaveRecordPageList(leaveRecordPageListBO.parse(), leaveRecordPageListBO);
        return page;
    }


    @Override
    public List<HrmEmployeeLeaveRecord> queryLeaveRecord(LocalDateTime leaveTime,Long employeeId) {
        return leaveRecordMapper.queryLeaveRecord(leaveTime,employeeId);
    }

    @Override
    public void addEmployeeLeaveRecord(SetEmployeeLeaveRecordBO employeeLeaveRecord) {
        HrmEmployeeLeaveRecord hrmEmployeeLeaveRecord = BeanUtil.copyProperties(employeeLeaveRecord, HrmEmployeeLeaveRecord.class);
        save(hrmEmployeeLeaveRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void queryOaLeaveExamineList() {
        HrmAttendanceExamine hrmAttendanceExamine = attendanceExamineService.queryHrmAttendanceExamine();
        if (ObjectUtil.isNotNull(hrmAttendanceExamine)) {
        }
    }

    @Override
    public HrmEmployeeLeaveRecord queryStartOrEndLeaveRecord(LocalDateTime currentDate, Long employeeId) {
        return leaveRecordMapper.queryStartOrEndLeaveRecord(currentDate, employeeId);
    }

    @Override
    public Integer queryLeaveEmpCount(String currentDate, Collection<Long> employeeIds) {
        queryOaLeaveExamineList();
        return baseMapper.queryLeaveEmpCount(currentDate, employeeIds);
    }
        @Autowired
        hrmAttendanceReportFieldRepository fieldRep;
    /**
     * 查询请假类型
     *
     * @return
     */
    @Override
    public List<String> queryLeaveTypeList() {
        List<String> leaveTypes =
                fieldRep.findAllByType(2).stream().map(f->f.getFieldName()).collect(Collectors.toList());
        return leaveTypes;
    }
}
