package com.tianye.hrsystem.controller;

import com.alibaba.fastjson.JSON;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.tianye.hrsystem.common.MonthlyFullSyncGuard;
import com.tianye.hrsystem.common.ProgressTracker;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.imple.HrmAttendanceDataServiceImpl;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.task.AttendanceSyncTaskLauncher;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmAttendanceDataService;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.util.MyDateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmAttendanceDataController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 14:12
 **/
@Controller
@RequestMapping("/attendanceData")
public class HrmAttendanceDataController {
    @Autowired
    IHrmAttendanceDataService dataService;

    @Autowired
    private com.tianye.hrsystem.task.AttendanceSyncTaskLauncher syncTaskLauncher;
    @Autowired
    hrmEmployeeRepository empRep;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    StringRedisTemplate redisRep;
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    Redis redis;
    @Autowired
    IWorkPlanService planService;
    @Autowired
    hrmAttendanceShiftRepository shiftRep;
    @Autowired
    hrmAttendanceGroupRepository groupRep;
    @Autowired
    hrmAttendancePlanRepository attendancePlanRep;

    @Autowired
    private com.tianye.hrsystem.service.IHrmAttendanceJudgeService attendanceJudgeService;

    @Autowired
    private MonthlyFullSyncGuard monthlyFullSyncGuard;

    /** 业务码：本月该范围已完成一次全量同步，禁止再次全量（前端据此弹提示引导定向补拉） */
    private static final int CODE_MONTHLY_FULL_DONE = 4001;
    /** 业务码：无可续传任务（「继续」时提示） */
    private static final int CODE_NOT_RESUMABLE = 4002;

    @PostMapping("/judgeQuery")
    @ResponseBody
    @io.swagger.annotations.ApiOperation("查询本地考勤判定结果（考勤汇总本地判定列数据源）")
    public successResult judgeQuery(String Begin, String End) {
        successResult result = new successResult();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date begin = StringUtils.isBlank(Begin) ? null : format.parse(Begin.trim());
            Date end = StringUtils.isBlank(End) ? null : format.parse(End.trim());
            if (begin == null || end == null) {
                throw new IllegalArgumentException("请提供 Begin/End（yyyy-MM-dd）");
            }
            result.setData(attendanceJudgeService.queryResults(begin, end, null));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/judgeRecompute")
    @ResponseBody
    @io.swagger.annotations.ApiOperation("本地考勤判定重算（弃用钉钉推送后的本地口径）")
    public successResult judgeRecompute(String Begin, String End) {
        successResult result = new successResult();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date begin = StringUtils.isBlank(Begin) ? null : format.parse(Begin.trim());
            Date end = StringUtils.isBlank(End) ? null : format.parse(End.trim());
            if (begin == null || end == null) {
                throw new IllegalArgumentException("请提供 Begin/End（yyyy-MM-dd）");
            }
            int written = attendanceJudgeService.recompute(begin, end, null);
            result.setMessage("本地判定完成，写库 " + written + " 行");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }



    private static final ThreadLocal<SimpleDateFormat> SS1 =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm"));

    private static final ThreadLocal<SimpleDateFormat> FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    Logger logger = LoggerFactory.getLogger(HrmAttendanceDataController.class);

    @RequestMapping("/sync")
    @ResponseBody
    public successResult GetData(String EmpID, String Begin, String End, String fullSync) {
        successResult result = new successResult();
        LoginUserInfo Info = CompanyContext.get();
        String companyId = Info != null && Info.getCompanyId() != null ? Info.getCompanyId() : "unknown";
        boolean lockHeld = false;
        try {
            // 自然月「每月一次」闸门：前端标记本次为覆盖全部员工的全量同步，且本月该范围已完成一次成功同步 → 拒绝，引导定向补拉
            if (Boolean.parseBoolean(fullSync)
                    && monthlyFullSyncGuard.isMonthlyFullSynced(MonthlyFullSyncGuard.BIZ_ATTENDANCE, companyId)) {
                result.setCode(CODE_MONTHLY_FULL_DONE);
                result.setMessage("本月该范围考勤数据已完成同步，如需补拉请先勾选指定员工");
                return result;
            }
            if (StringUtils.isBlank(EmpID)) {
                throw new IllegalArgumentException("EmpID不能为空");
            }
            if (StringUtils.isBlank(Begin) || StringUtils.isBlank(End)) {
                throw new IllegalArgumentException("Begin和End不能为空");
            }
            Date BeginDate = FORMAT.get().parse(Begin);
            Date EndDate = dateUtils.setItEnd(FORMAT.get().parse(End));
            // 按公司互斥：提交路径同步抢占，重复触发当场返回“进行中”
            if (!syncTaskLauncher.tryBegin(companyId, Info)) {
                // 任务正在进行中，返回特殊状态码202（Accepted），而不是抛异常
                // 前端会根据这个状态码自动切换到查看进度模式
                result.setCode(202);
                result.setMessage("考勤同步正在进行中，已自动切换到查看进度模式");
                Map<String, Object> data = new HashMap<>();
                data.put("alreadyRunning", true);
                data.put("queued", true);
                result.setData(data);
                logger.info("公司{}考勤同步正在进行中，用户点击按钮时自动切换到查看进度模式", companyId);
                return result;
            }
            lockHeld = true;
            long queuedAt = dataService.markSyncQueued();
            boolean submitted = syncTaskLauncher.submit(companyId, Info, () -> {
                try {
                    dataService.SyncDataWithAutoRetry(EmpID, BeginDate, EndDate);
                } catch (Exception e) {
                    logger.error("公司{}考勤同步后台任务失败: {}", companyId, e.getMessage(), e);
                }
            });
            if (!submitted) {
                throw new IllegalStateException("当前同步任务较多，请稍后再试");
            }
            // 已移交后台任务，由后台任务 finally 归还公司锁
            lockHeld = false;
            // 本次为全量同步且已通过月锁检查 → 写「全量待标记」，后台同步成功时据此打本月月锁
            if (Boolean.parseBoolean(fullSync)) {
                try {
                    redis.setex("attendance:sync:fullsync_pending:" + companyId, 86400, String.valueOf(System.currentTimeMillis()));
                } catch (Exception e) {
                    logger.warn("公司{}写全量待标记失败", companyId, e);
                }
            }
            logger.info("公司{}考勤同步已提交后台执行: {} ~ {}", companyId, Begin, End);
            result.setMessage("考勤同步已开始，请通过进度条查看进展");
            Map<String, Object> data = new HashMap<>();
            data.put("queued", true);
            data.put("queuedAt", queuedAt);
            result.setData(data);
        } catch (Exception ax) {
            if (lockHeld) {
                syncTaskLauncher.finish(companyId);
            }
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/continueSync")
    @ResponseBody
    public successResult continueSync() {
        successResult result = new successResult();
        LoginUserInfo Info = CompanyContext.get();
        String companyId = Info != null && Info.getCompanyId() != null ? Info.getCompanyId() : "unknown";
        boolean lockHeld = false;
        try {
            // 读上次失败同步的断点参数(EmpIDs|yyyy-MM-dd|yyyy-MM-dd)；考勤仅在终态失败(status=FAILED)才可续
            Map<String, Object> progress = dataService.getSyncProgress();
            boolean failed = progress != null
                    && ProgressTracker.STATUS_FAILED.equals(progress.get("status"));
            String params = progress != null ? String.valueOf(progress.get("params") == null ? "" : progress.get("params")) : "";
            if (!failed || StringUtils.isBlank(params)) {
                result.setCode(CODE_NOT_RESUMABLE);
                result.setMessage("暂无可继续的任务，请重新发起同步");
                return result;
            }
            String[] parts = params.split("\\|");
            if (parts.length != 3 || StringUtils.isBlank(parts[0])
                    || StringUtils.isBlank(parts[1]) || StringUtils.isBlank(parts[2])) {
                throw new IllegalStateException("断点参数不完整，请重新发起同步");
            }
            String empIds = parts[0].trim();
            Date beginDate = FORMAT.get().parse(parts[1].trim());
            Date endDate = dateUtils.setItEnd(FORMAT.get().parse(parts[2].trim()));
            // 按公司互斥：提交路径同步抢占
            if (!syncTaskLauncher.tryBegin(companyId, Info)) {
                result.setCode(202);
                result.setMessage("考勤同步正在进行中，已自动切换到查看进度模式");
                Map<String, Object> data = new HashMap<>();
                data.put("alreadyRunning", true);
                data.put("queued", true);
                result.setData(data);
                return result;
            }
            lockHeld = true;
            long queuedAt = dataService.markSyncQueued();
            boolean submitted = syncTaskLauncher.submit(companyId, Info, () -> {
                try {
                    dataService.SyncDataWithAutoRetry(empIds, beginDate, endDate);
                } catch (Exception e) {
                    logger.error("公司{}考勤同步(继续)后台任务失败: {}", companyId, e.getMessage(), e);
                }
            });
            if (!submitted) {
                throw new IllegalStateException("当前同步任务较多，请稍后再试");
            }
            // 已移交后台任务，由后台任务 finally 归还公司锁
            lockHeld = false;
            logger.info("公司{}考勤同步(继续)已提交后台执行: empIds={} ~ {}", companyId, empIds, parts[2]);
            result.setMessage("考勤同步已开始，请通过进度条查看进展");
            Map<String, Object> data = new HashMap<>();
            data.put("queued", true);
            data.put("queuedAt", queuedAt);
            result.setData(data);
        } catch (Exception ax) {
            if (lockHeld) {
                syncTaskLauncher.finish(companyId);
            }
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/syncAll")
    @ResponseBody
    public successResult SyncAll(String Begin, String End) {
        successResult result = new successResult();
        LoginUserInfo Info = CompanyContext.get();
        String companyId = Info != null && Info.getCompanyId() != null ? Info.getCompanyId() : "unknown";
        boolean lockHeld = false;
        try {
            if (StringUtils.isBlank(Begin) || StringUtils.isBlank(End)) {
                throw new IllegalArgumentException("Begin和End不能为空");
            }
            Date BeginDate = FORMAT.get().parse(Begin);
            Date EndDate = dateUtils.setItEnd(FORMAT.get().parse(End));
            List<HrmEmployee> alls = empRep.findAll().stream().collect(Collectors.toList());
            String EmpID =
                    StringUtils.join(alls.stream().map(f -> Long.toString(f.getEmployeeId())).collect(Collectors.toList()), ',');
            // 按公司互斥：提交路径同步抢占，重复触发当场返回“进行中”
            if (!syncTaskLauncher.tryBegin(companyId, Info)) {
                // 任务正在进行中，返回特殊状态码202（Accepted），而不是抛异常
                // 前端会根据这个状态码自动切换到查看进度模式
                result.setCode(202);
                result.setMessage("考勤同步正在进行中，已自动切换到查看进度模式");
                Map<String, Object> data = new HashMap<>();
                data.put("alreadyRunning", true);
                data.put("queued", true);
                result.setData(data);
                logger.info("公司{}考勤同步正在进行中，用户点击按钮时自动切换到查看进度模式", companyId);
                return result;
            }
            lockHeld = true;
            long queuedAt = dataService.markSyncQueued();
            final String empIdCsv = EmpID;
            boolean submitted = syncTaskLauncher.submit(companyId, Info, () -> {
                try {
                    dataService.SyncDataWithAutoRetry(empIdCsv, BeginDate, EndDate);
                } catch (Exception e) {
                    logger.error("公司{}考勤同步后台任务失败: {}", companyId, e.getMessage(), e);
                }
            });
            if (!submitted) {
                throw new IllegalStateException("当前同步任务较多，请稍后再试");
            }
            // 已移交后台任务，由后台任务 finally 归还公司锁
            lockHeld = false;
            logger.info("公司{}考勤同步(全员{})已提交后台执行: {} ~ {}", companyId, alls.size(), Begin, End);
            result.setMessage("考勤同步已开始，请通过进度条查看进展");
            Map<String, Object> data = new HashMap<>();
            data.put("queued", true);
            data.put("queuedAt", queuedAt);
            result.setData(data);
        } catch (Exception ax) {
            if (lockHeld) {
                syncTaskLauncher.finish(companyId);
            }
            logger.error("发起考勤同步失败", ax);
            result.raiseException(new Exception(HrmAttendanceDataServiceImpl.toFriendlySyncErrorMessage(ax)));
        }
        return result;
    }

    @RequestMapping("/getSyncProgress")
    @ResponseBody
    public successResult getSyncProgress() {
        successResult result = new successResult();
        try {
            result.setData(dataService.getSyncProgress());
        } catch (Exception ax) {
            logger.error("同步全部员工考勤失败", ax);
            result.raiseException(new Exception(HrmAttendanceDataServiceImpl.toFriendlySyncErrorMessage(ax)));
        }
        return result;
    }

    private String buildDuplicateSyncMessage(String companyId, LoginUserInfo currentUser) {
        Map<String, Object> owner = syncTaskLauncher.getRunningOwner(companyId);
        String companyName = firstText(owner.get("companyName"));
        if (StringUtils.isBlank(companyName) && currentUser != null) {
            companyName = currentUser.getCompanyName();
        }
        if (StringUtils.isBlank(companyName)) {
            companyName = "企业(" + companyId + ")";
        }
        String account = firstText(owner.get("account"));
        String userName = firstText(owner.get("userName"));
        String operator = StringUtils.isBlank(account) ? "未知账号" : account;
        if (!StringUtils.isBlank(userName) && !userName.equals(account)) {
            operator += "（" + userName + "）";
        }
        
        // 获取更详细的锁信息
        long acquiredAt = Long.parseLong(firstText(owner.get("acquiredAt")));
        long now = System.currentTimeMillis();
        long lockAge = now - acquiredAt;
        
        // 获取进度状态
        String status = firstText(redis.get("attendance:sync:" + companyId + ":status"));
        String updateTime = firstText(redis.get("attendance:sync:" + companyId + ":update_time"));
        
        StringBuilder message = new StringBuilder();
        message.append("企业“").append(companyName).append("”（ID: ").append(companyId).append("）的考勤数据同步正在进行中");
        message.append("，当前操作账号：").append(operator);
        message.append("；锁已存在 ").append(lockAge / 1000).append(" 秒");
        
        if (!status.isEmpty()) {
            message.append("，进度状态：").append(status);
        }
        if (!updateTime.isEmpty()) {
            long updateTs = Long.parseLong(updateTime);
            message.append("，最后更新：").append((now - updateTs) / 1000).append(" 秒前");
        }
        
        message.append("；请勿重复发起，可在进度条中查看当前进展");
        
        // 记录详细日志
        logger.warn("重复同步请求: companyId={}, operator={}, lockAge={}ms, status={}, updateTime={}", 
                    companyId, operator, lockAge, status, updateTime);
        
        return message.toString();
    }

    private String firstText(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text;
    }


    // 【2026-09-08 下线】原 /attendanceData/getOverTime 逐全员调钉钉 getcolumnval（硬编码加班列），
    // 无缓存无锁、不走用量日志（隐形消耗）、且经核实无任何前端/后端调用方（死端点）。
    // 加班合计已由本地加班统计模块（hrm_employee_over_time_record）承接，故直接删除，不再调钉钉。

    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * 返回所有的班次列表,用于选择
     *
     * @return
     */
    @RequestMapping("getShiftList")
    @ResponseBody
    public successResult getShiftList() {
        successResult result = new successResult();
        try {
            List<OapiAttendanceShiftListResponse.TopMinimalismShiftVo> shifts = shiftRep.findAll().stream()
                    .filter(shift -> shift != null && shift.getShiftId() != null
                            && StringUtils.isNotBlank(shift.getShiftName()))
                    .map(shift -> {
                        OapiAttendanceShiftListResponse.TopMinimalismShiftVo item =
                                new OapiAttendanceShiftListResponse.TopMinimalismShiftVo();
                        item.setId(shift.getShiftId());
                        item.setName(shift.getShiftName());
                        return item;
                    })
                    .collect(Collectors.toList());
            result.setData(shifts);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * 返回带有排班的信息的考勤组。有于首页展示
     *
     * @return
     */
    @RequestMapping("/getGoupList")
    @ResponseBody
    public successResult getGroupList() {
        successResult result = new successResult();
        try {
            List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> Res = planService.getAllGroupsForDisplay();
            List<GroupObject> res = new ArrayList<>();
            if (Res.size() > 0) {
                List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> DD =
                        Res.stream().filter(f -> f.getType().equals("TURN")).collect(Collectors.toList());
                DD.forEach(f -> {
                    GroupObject obj = new GroupObject();
                    obj.setGroupId(f.getGroupId());
                    obj.setGroupName(f.getGroupName());
                    obj.setMemberCount(f.getMemberCount());

                    List<ComboboxItem> groupItems = new ArrayList<>();
                    ComboboxItem sItem = new ComboboxItem();
                    sItem.setId("1");
                    sItem.setText("休假");
                    groupItems.add(sItem);
                    List<ComboboxItem> iits = new ArrayList<>();
                    List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> selectedClasses = f.getSelectedClass();
                    if (selectedClasses == null) {
                        selectedClasses = Collections.emptyList();
                    }
                    selectedClasses.forEach(x -> {
                        if (x == null || x.getClassId() == null) {
                            return;
                        }
                        String Id = Long.toString(x.getClassId());
                        Optional<ComboboxItem> findItems = groupItems.stream().filter(y -> y.getId().equals(Id)).findFirst();
                        if (findItems.isPresent() == false) {
                            ComboboxItem item = new ComboboxItem();
                            item.setId(Id);
                            item.setText(x.getClassName());
                            applySectionTimesToComboboxItem(item, x.getSections());
                            groupItems.add(item);
                            iits.add(item);
                        }
                    });
                    String Key = "ClassList_" + Long.toString(f.getGroupId());
                    redisRep.opsForValue().set(Key, JSON.toJSONString(groupItems), 30, TimeUnit.MINUTES);
                    obj.setShifts(iits);
                    res.add(obj);
                });
                result.setData(res);
            }
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("/getUsersByGroup")
    public successResult getUsersByGroup(String GroupID) {
        successResult result = new successResult();
        try {
            LoginUserInfo Info = CompanyContext.get();
            List<UserObject> users = planService.getUsersForDisplay(Info.getCompanyId());
            List<ComboboxItem> res = new ArrayList<>();
            users.stream()
                    .filter(f -> f != null && StringUtils.equals(GroupID, f.getGroupId()))
                    .forEach(user -> {
                        ComboboxItem item = new ComboboxItem();
                        item.setId(user.getId());
                        item.setText(user.getName());
                        res.add(item);
                    });
            result.setData(res);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("/getClassListByGroup")
    public successResult getClassListByGroupId(String GroupID) {
        successResult result = new successResult();
        try {
            String Key = "ClassList_" + GroupID;
            if (redisRep.hasKey(Key)) {
                String X = redisRep.opsForValue().get(Key);
                List<ComboboxItem> Items = JSON.parseArray(X, ComboboxItem.class);
                result.setData(Items);
            } else {
                result.setData(buildLocalClassItemsByGroup(GroupID));
            }
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    private List<ComboboxItem> buildLocalClassItemsByGroup(String groupIdText) {
        List<ComboboxItem> items = new ArrayList<>();
        ComboboxItem restItem = new ComboboxItem();
        restItem.setId("1");
        restItem.setText("休假");
        items.add(restItem);

        Long groupId = parseLongOrNull(groupIdText);
        if (groupId == null) {
            return items;
        }
        Optional<HrmAttendanceGroup> localGroup = groupRep.findById(groupId);
        if (!localGroup.isPresent()) {
            localGroup = groupRep.findFirstByOldGroupId(groupId);
        }
        if (!localGroup.isPresent()) {
            return items;
        }
        HrmAttendanceGroup group = localGroup.get();
        Set<String> shiftIds = parseShiftSetting(group.getShiftSetting());
        if (shiftIds.isEmpty()) {
            return items;
        }
        Set<Long> allowedGroupIds = new LinkedHashSet<>();
        if (group.getAttendanceGroupId() != null) {
            allowedGroupIds.add(group.getAttendanceGroupId());
        }
        if (group.getOldGroupId() != null) {
            allowedGroupIds.add(group.getOldGroupId());
        }
        List<HrmAttendanceShift> shifts = shiftRep.findAll();
        if (shifts == null || shifts.isEmpty()) {
            return items;
        }
        for (HrmAttendanceShift shift : shifts) {
            if (shift == null || shift.getShiftId() == null || StringUtils.isBlank(shift.getShiftName())
                    || !shiftIds.contains(String.valueOf(shift.getShiftId()))
                    || (shift.getGroupId() != null && !allowedGroupIds.contains(shift.getGroupId()))) {
                continue;
            }
            ComboboxItem item = new ComboboxItem();
            item.setId(String.valueOf(shift.getShiftId()));
            item.setText(shift.getShiftName());
            item.setBegin1(normalizeShiftTime(shift.getStart1()));
            item.setEnd1(normalizeShiftTime(shift.getEnd1()));
            item.setBegin2(normalizeShiftTime(shift.getStart2()));
            item.setEnd2(normalizeShiftTime(shift.getEnd2()));
            items.add(item);
        }
        return items;
    }

    private static final ThreadLocal<SimpleDateFormat> SIMPLE2 =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    @RequestMapping("/getPlanDataByGroup")
    @ResponseBody
    public successResult getPlanDataByGroup(String Begin, String End, String GroupID) {
        successResult result = new successResult();
        try {
            if (StringUtils.isBlank(Begin) || StringUtils.isBlank(End)) {
                throw new Exception("请同时指明开始(Begin)和结束(End)时间!");
            }
            Date begin = startOfDay(SIMPLE2.get().parse(Begin));
            Date end = endOfDay(SIMPLE2.get().parse(End));
            Long groupId = parseLongOrNull(GroupID);
            List<PlanObject> res = buildLocalPlanDataByGroup(begin, end, groupId);
            result.setData(res);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.raiseException(ax);
        }
        return result;
    }

    private List<PlanObject> buildLocalPlanDataByGroup(Date begin, Date end, Long groupId) {
        List<HrmAttendancePlan> plans = attendancePlanRep.findAllByWorkDateBetween(begin, end);
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> allowedGroupIds = resolveLocalGroupIds(groupId);
        Map<String, tbattendanceuser> usersByDingTalkId = userRep.findAll().stream()
                .filter(item -> item != null && StringUtils.isNotBlank(item.getUserId()))
                .collect(Collectors.toMap(tbattendanceuser::getUserId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, HrmAttendancePlan> preferredPlans = new LinkedHashMap<>();
        for (HrmAttendancePlan plan : plans) {
            if (plan == null || plan.getClassId() == null || plan.getWorkDate() == null
                    || StringUtils.isBlank(plan.getUserId())) {
                continue;
            }
            if (!allowedGroupIds.isEmpty() && !allowedGroupIds.contains(plan.getGroupId())) {
                continue;
            }
            Long numericUserId = parseLongOrNull(plan.getUserId());
            if (numericUserId == null) {
                continue;
            }
            tbattendanceuser user = usersByDingTalkId.get(plan.getUserId());
            if (user == null) {
                continue;
            }
            String workDate = SIMPLE2.get().format(plan.getWorkDate());
            String key = plan.getUserId() + "#" + plan.getClassId() + "#" + workDate;
            HrmAttendancePlan existing = preferredPlans.get(key);
            if (existing == null || isBetterDisplayPlan(plan, existing)) {
                preferredPlans.put(key, plan);
            }
        }
        List<PlanObject> result = new ArrayList<>();
        for (HrmAttendancePlan plan : preferredPlans.values()) {
            tbattendanceuser user = usersByDingTalkId.get(plan.getUserId());
            PlanObject object = new PlanObject();
            object.setPlanId(plan.getPlanId());
            object.setGroupId(plan.getGroupId());
            object.setShiftId(plan.getClassId());
            object.setUserId(parseLongOrNull(plan.getUserId()));
            object.setUserName(user.getUserName());
            object.setWorkDate(SIMPLE2.get().format(plan.getWorkDate()));
            result.add(object);
        }
        return result;
    }

    private Set<Long> resolveLocalGroupIds(Long groupId) {
        if (groupId == null) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        result.add(groupId);
        groupRep.findById(groupId).ifPresent(group -> {
            if (group.getOldGroupId() != null) {
                result.add(group.getOldGroupId());
            }
        });
        groupRep.findFirstByOldGroupId(groupId).ifPresent(group -> {
            if (group.getAttendanceGroupId() != null) {
                result.add(group.getAttendanceGroupId());
            }
        });
        return result;
    }

    private boolean isBetterDisplayPlan(HrmAttendancePlan current, HrmAttendancePlan existing) {
        if ("OffDuty".equals(current.getCheckType()) && !"OffDuty".equals(existing.getCheckType())) {
            return true;
        }
        Date currentTime = current.getPlanCheckTime();
        Date existingTime = existing.getPlanCheckTime();
        return currentTime != null && (existingTime == null || currentTime.after(existingTime));
    }

    private Date startOfDay(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date endOfDay(Date value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private Long parseLongOrNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @RequestMapping("/getAllShifts")
    @ResponseBody
    public successResult getAllShiftList() {
        successResult result = new successResult();
        try {
            Map<String, ShiftItem> localShiftItems = buildValidLocalShiftItems();
            List<ShiftItem> items = buildShiftItemsFromDisplayGroups(localShiftItems.keySet());
            if (items.size() <= 1) {
                items = buildShiftItemsFromLocalSnapshot(localShiftItems);
            }
            result.setData(items);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    private List<ShiftItem> buildShiftItemsFromLocalSnapshot(Map<String, ShiftItem> localShiftItems) {
        List<ShiftItem> items = new ArrayList<>();
        items.add(buildRestShiftItem());
        items.addAll(localShiftItems.values());
        return items;
    }

    private List<ShiftItem> buildShiftItemsFromDisplayGroups(Set<String> activeLocalShiftKeys) throws Exception {
        List<ShiftItem> items = new ArrayList<>();
        items.add(buildRestShiftItem());

        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups = planService.getAllGroupsForDisplay();
        if (groups == null || groups.isEmpty()) {
            return items;
        }
        Map<String, ShiftItem> deduplicated = new LinkedHashMap<>();
        groups.forEach(group -> {
            if (!isTurnGroup(group) || group.getGroupId() == null || StringUtils.isBlank(group.getGroupName())) {
                return;
            }
            String groupName = group.getGroupName();
            Long groupId = group.getGroupId();
            List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> selectedClasses = group.getSelectedClass();
            if (selectedClasses == null) {
                selectedClasses = Collections.emptyList();
            }
            selectedClasses.forEach(shiftClass -> {
                if (shiftClass == null || shiftClass.getClassId() == null || StringUtils.isBlank(shiftClass.getClassName())) {
                    return;
                }
                String shiftId = Long.toString(shiftClass.getClassId());
                String groupIdText = groupId == null ? null : Long.toString(groupId);
                String shiftKey = buildShiftKey(groupIdText, shiftId);
                if (!activeLocalShiftKeys.isEmpty() && !activeLocalShiftKeys.contains(shiftKey)) {
                    return;
                }
                if (deduplicated.containsKey(shiftKey)) {
                    return;
                }
                ShiftItem item = new ShiftItem();
                item.setGroupId(groupIdText);
                item.setGroupName(buildShiftDisplayLabel(groupName, shiftClass.getClassName()));
                item.setShiftId(shiftId);
                item.setShiftName(shiftClass.getClassName());
                applySectionTimesToShiftItem(item, shiftClass.getSections());
                if (!hasAnyShiftTime(item)) {
                    return;
                }
                deduplicated.put(shiftKey, item);
            });
        });
        items.addAll(deduplicated.values());
        return items;
    }

    private Map<String, ShiftItem> buildValidLocalShiftItems() {
        Map<String, ShiftItem> items = new LinkedHashMap<>();
        Map<Long, LocalGroupSnapshot> groups = buildLocalGroupSnapshots();
        if (groups.isEmpty()) {
            return items;
        }
        List<HrmAttendanceShift> shifts = shiftRep.findAll();
        if (shifts == null || shifts.isEmpty()) {
            return items;
        }
        for (HrmAttendanceShift shift : shifts) {
            if (shift == null || shift.getShiftId() == null || shift.getGroupId() == null
                    || StringUtils.isBlank(shift.getShiftName())) {
                continue;
            }
            LocalGroupSnapshot group = groups.get(shift.getGroupId());
            if (group == null) {
                continue;
            }
            String shiftId = String.valueOf(shift.getShiftId());
            if (!group.getShiftIds().contains(shiftId)) {
                continue;
            }
            ShiftItem item = new ShiftItem();
            item.setShiftId(shiftId);
            item.setShiftName(shift.getShiftName());
            item.setGroupId(String.valueOf(shift.getGroupId()));
            item.setGroupName(buildShiftDisplayLabel(group.getGroupName(), shift.getShiftName()));
            item.setBegin1(normalizeShiftTime(shift.getStart1()));
            item.setEnd1(normalizeShiftTime(shift.getEnd1()));
            item.setBegin2(normalizeShiftTime(shift.getStart2()));
            item.setEnd2(normalizeShiftTime(shift.getEnd2()));
            item.setBegin3(normalizeShiftTime(shift.getStart3()));
            item.setEnd3(normalizeShiftTime(shift.getEnd3()));
            if (!hasAnyShiftTime(item)) {
                continue;
            }
            items.putIfAbsent(buildShiftKey(item.getGroupId(), item.getShiftId()), item);
        }
        return items;
    }

    private Map<Long, LocalGroupSnapshot> buildLocalGroupSnapshots() {
        Map<Long, LocalGroupSnapshot> groups = new HashMap<>();
        List<HrmAttendanceGroup> snapshotGroups = groupRep.findAll();
        if (snapshotGroups == null || snapshotGroups.isEmpty()) {
            return groups;
        }
        for (HrmAttendanceGroup group : snapshotGroups) {
            if (group == null || StringUtils.isBlank(group.getName())) {
                continue;
            }
            Set<String> shiftIds = parseShiftSetting(group.getShiftSetting());
            if (shiftIds.isEmpty()) {
                continue;
            }
            LocalGroupSnapshot snapshot = new LocalGroupSnapshot(group.getName(), shiftIds);
            if (group.getAttendanceGroupId() != null) {
                groups.put(group.getAttendanceGroupId(), snapshot);
            }
            if (group.getOldGroupId() != null) {
                groups.putIfAbsent(group.getOldGroupId(), snapshot);
            }
        }
        return groups;
    }

    private Set<String> parseShiftSetting(String shiftSetting) {
        if (StringUtils.isBlank(shiftSetting)) {
            return Collections.emptySet();
        }
        Set<String> shiftIds = new LinkedHashSet<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < shiftSetting.length(); i++) {
            char value = shiftSetting.charAt(i);
            if (Character.isDigit(value)) {
                current.append(value);
                continue;
            }
            if (current.length() > 0) {
                shiftIds.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            shiftIds.add(current.toString());
        }
        return shiftIds;
    }

    private boolean isTurnGroup(OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group) {
        return group != null && StringUtils.equals("TURN", group.getType());
    }

    private boolean hasAnyShiftTime(ShiftItem item) {
        return hasShiftRange(item.getBegin1(), item.getEnd1())
                || hasShiftRange(item.getBegin2(), item.getEnd2())
                || hasShiftRange(item.getBegin3(), item.getEnd3());
    }

    private boolean hasShiftRange(String begin, String end) {
        return StringUtils.isNotBlank(begin) && StringUtils.isNotBlank(end);
    }

    private String buildShiftKey(String groupId, String shiftId) {
        return StringUtils.defaultString(groupId) + "#" + StringUtils.defaultString(shiftId);
    }

    private ShiftItem buildRestShiftItem() {
        ShiftItem restItem = new ShiftItem();
        restItem.setShiftId("1");
        restItem.setShiftName("休假");
        return restItem;
    }

    private static class LocalGroupSnapshot {
        private final String groupName;
        private final Set<String> shiftIds;

        private LocalGroupSnapshot(String groupName, Set<String> shiftIds) {
            this.groupName = groupName;
            this.shiftIds = shiftIds;
        }

        private String getGroupName() {
            return groupName;
        }

        private Set<String> getShiftIds() {
            return shiftIds;
        }
    }

    private void applySectionTimesToComboboxItem(ComboboxItem item,
            List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> sections) {
        List<String[]> sectionTimes = extractSectionTimes(sections);
        if (sectionTimes.size() > 0) {
            item.setBegin1(sectionTimes.get(0)[0]);
            item.setEnd1(sectionTimes.get(0)[1]);
        }
        if (sectionTimes.size() > 1) {
            item.setBegin2(sectionTimes.get(1)[0]);
            item.setEnd2(sectionTimes.get(1)[1]);
        }
    }

    private void applySectionTimesToShiftItem(ShiftItem item,
            List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> sections) {
        List<String[]> sectionTimes = extractSectionTimes(sections);
        if (sectionTimes.size() > 0) {
            item.setBegin1(sectionTimes.get(0)[0]);
            item.setEnd1(sectionTimes.get(0)[1]);
        }
        if (sectionTimes.size() > 1) {
            item.setBegin2(sectionTimes.get(1)[0]);
            item.setEnd2(sectionTimes.get(1)[1]);
        }
        if (sectionTimes.size() > 2) {
            item.setBegin3(sectionTimes.get(2)[0]);
            item.setEnd3(sectionTimes.get(2)[1]);
        }
    }

    private List<String[]> extractSectionTimes(List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> sections) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }
        List<String[]> result = new ArrayList<>();
        for (OapiAttendanceGetsimplegroupsResponse.AtSectionVo section : sections) {
            if (section == null || section.getTimes() == null || section.getTimes().isEmpty()) {
                continue;
            }
            Optional<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> onDuty = section.getTimes()
                    .stream()
                    .filter(time -> time != null && "OnDuty".equals(time.getCheckType()) && time.getCheckTime() != null)
                    .findFirst();
            Optional<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> offDuty = section.getTimes()
                    .stream()
                    .filter(time -> time != null && "OffDuty".equals(time.getCheckType()) && time.getCheckTime() != null)
                    .findFirst();
            if (!onDuty.isPresent() || !offDuty.isPresent()) {
                continue;
            }
            result.add(new String[] {
                    SS1.get().format(onDuty.get().getCheckTime()),
                    SS1.get().format(offDuty.get().getCheckTime())
            });
        }
        return result;
    }

    private String normalizeShiftTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String[] parts = value.split(":");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1];
        }
        return parts[0];
    }

    private String buildShiftDisplayLabel(String groupName, String shiftName) {
        if (StringUtils.isBlank(groupName)) {
            return shiftName;
        }
        if (StringUtils.isBlank(shiftName) || StringUtils.equals(groupName, shiftName)) {
            return groupName;
        }
        return groupName + " / " + shiftName;
    }
    @ResponseBody
    @RequestMapping("/getAllUsers")
    public successResult getAllUsers() {
        successResult result = new successResult();
        try {
            LoginUserInfo Info=CompanyContext.get();
            List<UserObject> res=planService.getUsersForDisplay(Info.getCompanyId());
            result.setData(res);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 锁健康检查：检查并清理残留的同步锁
     * 手动调用，用于清理因异常未释放的锁
     */
    @RequestMapping("/checkStaleLocks")
    @ResponseBody
    public successResult checkStaleLocks() {
        successResult result = new successResult();
        try {
            // 获取当前进程启动时间
            long processStartedAt = System.currentTimeMillis();
            
            // 检查Redis中的锁
            String lockPattern = AttendanceSyncTaskLauncher.RUNNING_LOCK_PREFIX + "*";
            Set<Object> keys = redis.keys(lockPattern);
            
            List<Map<String, Object>> cleanedLocks = new ArrayList<>();
            if (keys != null && !keys.isEmpty()) {
                for (Object keyObj : keys) {
                    String key = String.valueOf(keyObj);
                    String companyId = key.replace(AttendanceSyncTaskLauncher.RUNNING_LOCK_PREFIX, "");
                    Map<String, Object> owner = syncTaskLauncher.getRunningOwner(companyId);
                    long acquiredAt = Long.parseLong(String.valueOf(owner.getOrDefault("acquiredAt", 0)));
                    
                    // 如果锁存在超过5分钟，清理它
                    if (acquiredAt > 0 && (processStartedAt - acquiredAt) > 5 * 60 * 1000) {
                        syncTaskLauncher.finish(companyId);
                        Map<String, Object> cleaned = new HashMap<>();
                        cleaned.put("companyId", companyId);
                        cleaned.put("acquiredAt", acquiredAt);
                        cleaned.put("lockAge", processStartedAt - acquiredAt);
                        cleanedLocks.add(cleaned);
                        logger.info("锁健康检查: 清理残留锁, companyId={}, lockAge={}ms", companyId, processStartedAt - acquiredAt);
                    }
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("totalLocks", keys != null ? keys.size() : 0);
            data.put("cleanedLocks", cleanedLocks);
            data.put("cleanedCount", cleanedLocks.size());
            result.setData(data);
            
            logger.info("锁健康检查完成: 总锁数={}, 清理数={}", keys != null ? keys.size() : 0, cleanedLocks.size());
        } catch (Exception ax) {
            logger.error("锁健康检查失败", ax);
            result.raiseException(ax);
        }
        return result;
    }
}
