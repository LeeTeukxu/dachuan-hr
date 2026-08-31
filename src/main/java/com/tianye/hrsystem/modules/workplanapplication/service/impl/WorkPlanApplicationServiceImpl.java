package com.tianye.hrsystem.modules.workplanapplication.service.impl;

import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.model.HrmWorkplanApplication;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.modules.workplanapplication.service.IWorkPlanApplicationService;
import com.tianye.hrsystem.repository.hrmWorkplanApplicationRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IWorkPlanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 排班修改申请·审批实现
 */
@Service
public class WorkPlanApplicationServiceImpl implements IWorkPlanApplicationService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";

    @Autowired
    private hrmWorkplanApplicationRepository applicationRepository;

    @Autowired
    private tbattendanceuserRepository attendanceUserRepository;

    @Autowired
    private IWorkPlanService workPlanService;

    @Override
    public HrmWorkplanApplication submit(Long employeeId, Date workDate, String shiftType,
                                         String customStart, String customEnd,
                                         String customShiftPeriod, Boolean customContinuousShift,
                                         String restShiftType, String remark) throws Exception {
        if (employeeId == null) {
            throw new Exception("employeeId不能为空");
        }
        if (workDate == null) {
            throw new Exception("排班日期不能为空");
        }
        String normalizedShiftType = normalizeShiftType(shiftType);
        if (StringUtils.isBlank(normalizedShiftType)) {
            throw new Exception("班次类型不能为空");
        }
        if (!"rest".equals(normalizedShiftType)
                && (StringUtils.isBlank(customStart) || StringUtils.isBlank(customEnd))) {
            throw new Exception("自定义班次必须填写上下班时间");
        }

        tbattendanceuser attendanceUser = resolveAttendanceUser(employeeId);

        HrmWorkplanApplication app = new HrmWorkplanApplication();
        app.setEmployeeId(employeeId);
        app.setUserId(attendanceUser.getUserId());
        app.setGroupId(attendanceUser.getGroupId() == null ? null : String.valueOf(attendanceUser.getGroupId()));
        app.setWorkDate(workDate);
        app.setShiftType(normalizedShiftType);
        app.setCustomStart("rest".equals(normalizedShiftType) ? null : customStart);
        app.setCustomEnd("rest".equals(normalizedShiftType) ? null : customEnd);
        app.setCustomShiftPeriod("rest".equals(normalizedShiftType) ? null : customShiftPeriod);
        app.setCustomContinuousShift(Boolean.TRUE.equals(customContinuousShift));
        app.setRestShiftType("rest".equals(normalizedShiftType) ? restShiftType : null);
        app.setStatus(STATUS_PENDING);
        app.setIsCurrentEffective(false);
        app.setRemark(remark);
        app.setCreateTime(new Date());
        return applicationRepository.save(app);
    }

    @Override
    public List<HrmWorkplanApplication> listMy(Long employeeId, String status) {
        if (StringUtils.isNotBlank(status)) {
            return applicationRepository.findByEmployeeIdAndStatusOrderByCreateTimeDesc(employeeId, status);
        }
        return applicationRepository.findByEmployeeIdOrderByCreateTimeDesc(employeeId);
    }

    @Override
    public void cancel(Long employeeId, Long applicationId) throws Exception {
        HrmWorkplanApplication app = findOwn(employeeId, applicationId);
        if (!STATUS_PENDING.equals(app.getStatus())) {
            throw new Exception("仅待审批的申请可撤销");
        }
        app.setStatus(STATUS_CANCELLED);
        applicationRepository.save(app);
    }

    @Override
    public HrmWorkplanApplication findById(Long applicationId) throws Exception {
        if (applicationId == null) {
            throw new Exception("申请单ID不能为空");
        }
        Optional<HrmWorkplanApplication> byId = applicationRepository.findById(applicationId);
        if (!byId.isPresent()) {
            throw new Exception("申请单不存在");
        }
        return byId.get();
    }

    private HrmWorkplanApplication findOwn(Long employeeId, Long applicationId) throws Exception {
        HrmWorkplanApplication app = findById(applicationId);
        if (!app.getEmployeeId().equals(employeeId)) {
            throw new Exception("只能操作自己的申请");
        }
        return app;
    }

    @Override
    public List<HrmWorkplanApplication> listToApprove(Long approverEmployeeId, String scope) {
        if ("done".equalsIgnoreCase(scope)) {
            return applicationRepository.findApprovedByApprover(approverEmployeeId);
        }
        return applicationRepository.findToApproveByStatus(STATUS_PENDING, approverEmployeeId);
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayShiftVO approve(Long applicationId, String action, String reason) throws Exception {
        HrmWorkplanApplication app = findById(applicationId);
        if (!STATUS_PENDING.equals(app.getStatus())) {
            throw new Exception("该申请已被处理");
        }
        boolean approved = "approved".equalsIgnoreCase(action);
        if (approved) {
            WorkPlanEmployeeDayShiftVO dayShift = workPlanService.saveEmployeeDayShift(
                    app.getEmployeeId(), app.getWorkDate(),
                    app.getShiftType(),
                    app.getCustomStart(), app.getCustomEnd(),
                    app.getCustomShiftPeriod(), app.getCustomContinuousShift(),
                    app.getRestShiftType());
            app.setStatus(STATUS_APPROVED);
            app.setApproveTime(new Date());
            app.setIsCurrentEffective(true);
            markOthersNotEffective(app);
            applicationRepository.save(app);
            return dayShift;
        }
        app.setStatus(STATUS_REJECTED);
        app.setApproveTime(new Date());
        app.setRejectReason(reason);
        applicationRepository.save(app);
        return null;
    }

    /** 同日其他已通过申请标记为非当前生效 */
    private void markOthersNotEffective(HrmWorkplanApplication approved) {
        List<HrmWorkplanApplication> sameDay =
                applicationRepository.findByEmployeeIdAndWorkDate(approved.getEmployeeId(), approved.getWorkDate());
        for (HrmWorkplanApplication other : sameDay) {
            if (STATUS_APPROVED.equals(other.getStatus())
                    && !other.getId().equals(approved.getId())
                    && Boolean.TRUE.equals(other.getIsCurrentEffective())) {
                other.setIsCurrentEffective(false);
                applicationRepository.save(other);
            }
        }
    }

    @Override
    public List<HrmWorkplanApplication> listAll(String status, Date begin, Date end) {
        List<HrmWorkplanApplication> rows;
        if (StringUtils.isNotBlank(status)) {
            if (begin != null && end != null) {
                rows = applicationRepository.findByStatusAndWorkDateBetweenOrderByCreateTimeDesc(status, begin, end);
            } else {
                rows = applicationRepository.findByStatusOrderByCreateTimeDesc(status);
            }
        } else {
            if (begin != null && end != null) {
                rows = applicationRepository.findByWorkDateBetweenOrderByCreateTimeDesc(begin, end);
            } else {
                rows = applicationRepository.findAll();
                rows = rows.stream().sorted((a, b) -> {
                    Date at = a.getCreateTime();
                    Date bt = b.getCreateTime();
                    if (at == null) return 1;
                    if (bt == null) return -1;
                    return bt.compareTo(at);
                }).collect(Collectors.toList());
            }
        }
        return rows == null ? new ArrayList<>() : rows;
    }

    @Override
    public WorkPlanEmployeeDayShiftVO queryEmployeeDayShift(Long employeeId, Date workDate) throws Exception {
        return workPlanService.queryEmployeeDayShift(employeeId, workDate);
    }

    private tbattendanceuser resolveAttendanceUser(Long employeeId) throws Exception {
        Optional<tbattendanceuser> attendanceUser = attendanceUserRepository.findFirstByEmpId(employeeId);
        if (!attendanceUser.isPresent() || StringUtils.isBlank(attendanceUser.get().getUserId())) {
            throw new Exception("未找到员工对应的考勤用户，无法提交排班申请");
        }
        return attendanceUser.get();
    }

    private String normalizeShiftType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String text = value.trim().toLowerCase();
        if ("rest".equals(text) || "3".equals(text) || "调休".equals(text) || "休息".equals(text)) {
            return "rest";
        }
        if ("standard".equals(text) || "1".equals(text)) {
            return "standard";
        }
        return "custom";
    }
}