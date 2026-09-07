package com.tianye.hrsystem.modules.workplanapplication.service.impl;

import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmWorkplanApplication;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.modules.workplanapplication.service.IWorkPlanApplicationService;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmWorkplanApplicationRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IWorkPlanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private IWorkPlanService workPlanService;

    @Autowired
    private com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService miniAppPermissionService;

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
        boolean done = "done".equalsIgnoreCase(scope);
        // 可见范围：null=全部员工；其余按员工 ID 集合过滤（默认=直属下属，保持原有 parent_id 查询）
        List<Long> visibleIds = miniAppPermissionService.resolveVisibleEmployeeIds(approverEmployeeId);
        boolean unrestricted = visibleIds == null;
        List<Long> explicitIds = visibleIds == null ? new ArrayList<>() : visibleIds;
        MpScopeKind kind = resolveScopeKind(approverEmployeeId);
        if (unrestricted) {
            return done
                    ? applicationRepository.findByStatusNotOrderByApproveTimeDesc(STATUS_PENDING)
                    : applicationRepository.findByStatusOrderByCreateTimeDesc(STATUS_PENDING);
        }
        if (kind == MpScopeKind.DEFAULT_SUBORDINATES && explicitIds.isEmpty()) {
            // 无下属且无追加员工：保持原查询（done=我审批过的）
            return done
                    ? applicationRepository.findApprovedByApprover(approverEmployeeId)
                    : applicationRepository.findToApproveByStatus(STATUS_PENDING, approverEmployeeId);
        }
        // 直属下属档（∪追加员工）与自定义档：按可见员工集合查询
        if (!done) {
            return applicationRepository.findToApproveByStatusAndEmployeeIdIn(STATUS_PENDING, explicitIds);
        }
        // done 综合口径（2026-09-06 第二十七轮）：我亲手批过的 ∪ 可见范围内已处理的，按 id 去重、审批时间倒序
        Map<Long, HrmWorkplanApplication> merged = new LinkedHashMap<>();
        for (HrmWorkplanApplication app : applicationRepository.findApprovedByApprover(approverEmployeeId)) {
            merged.put(app.getId(), app);
        }
        for (HrmWorkplanApplication app : applicationRepository.findProcessedByEmployeeIdIn(explicitIds)) {
            merged.putIfAbsent(app.getId(), app);
        }
        List<HrmWorkplanApplication> result = new ArrayList<>(merged.values());
        result.sort((a, b) -> {
            Date ta = a.getApproveTime();
            Date tb = b.getApproveTime();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return result;
    }

    private enum MpScopeKind { DEFAULT_SUBORDINATES, ALL, CUSTOM }

    private MpScopeKind resolveScopeKind(Long approverEmployeeId) {
        com.tianye.hrsystem.model.MpSchedulePermission permission =
                miniAppPermissionService.getByEmployeeId(approverEmployeeId);
        if (permission == null || permission.getVisibleScope() == null) {
            return MpScopeKind.DEFAULT_SUBORDINATES;
        }
        if (permission.getVisibleScope() == com.tianye.hrsystem.model.MpSchedulePermission.SCOPE_CUSTOM) {
            return MpScopeKind.CUSTOM;
        }
        if (permission.getVisibleScope() == com.tianye.hrsystem.model.MpSchedulePermission.SCOPE_ALL) {
            return MpScopeKind.ALL;
        }
        return MpScopeKind.DEFAULT_SUBORDINATES;
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

    /**
     * 排班申请身份解析（2026-09 决议：不再读写 tbattendanceuser）。
     * userId 取 hrm_employee.dingtalk_user_id（缺失即明确报错），groupId 属钉钉考勤组数据不再落申请单。
     */
    private tbattendanceuser resolveAttendanceUser(Long employeeId) throws Exception {
        HrmEmployee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            throw new Exception("未找到员工[" + employeeId + "]的档案，无法提交排班申请");
        }
        String userId = StringUtils.trimToEmpty(employee.getDingtalkUserId());
        if (StringUtils.isBlank(userId)) {
            throw new Exception("员工[" + StringUtils.trimToEmpty(employee.getEmployeeName())
                    + "]缺少钉钉用户ID，无法提交排班申请，请先在钉钉创建该员工并使用员工管理的「重新映射」");
        }
        tbattendanceuser identity = new tbattendanceuser();
        identity.setEmpId(employeeId);
        identity.setUserId(userId);
        identity.setUserName(employee.getEmployeeName());
        identity.setDepId(employee.getDeptId());
        return identity;
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