# 加班/夜班统计 与 考勤汇总

菜单前缀：`/hrmOvertimeNightStatistics`、`/hrmProduceAttendance`（考勤汇总，同步方向固定"加班/夜班统计 → 考勤汇总"）。

## 需求要点
- 加班/夜班资格（2026-09-06 起全局口径）：`affiliation_system=2`（生产体系）**或** `rest_type=2`（固定月休4天）员工统计加班小时/加班费/夜班次数/夜班补贴；其余员工这些字段强制为 0，但应出勤/实际出勤/应计出勤链路保留。
- 应出勤班制按 `rest_type` 优先：`rest_type=2` 按"自然月天数 − 基本工资设置 `productionMonthlyRestDays`（默认 4，不另扣法定节假日）"；`rest_type=1` 按单双休月历 `workDays`；`rest_type` 为空才按 `affiliation_system` 兼容旧分流。薪资核算的应出勤唯一来源是本模块落库 `expected_attendance_days`，缺失即阻断并提示维护"单双休设置"。
- 实际出勤 = `应出勤小时 + 可计入加班小时 − 事假/病假/调休/年假扣减`；行政体系可计入加班仅本地审批加班（不含日级自动加班），其他体系用月度加班汇总；`外出/出差/补卡` 不扣减；展示加班 `overtimeHours` 与可计入加班 `attendanceOvertimeHours` 拆分。
- 应计出勤 = `实际出勤 + 调休 + 年假 − 加班 + 病假`（本地审批口径）。
- 审批优先：目标月存在本地加班审批时按审批时长日累计覆盖 `overtimeHours`；`statisticsStatus=取消至统计` 的审批在加班汇总、实际/应计出勤扣减中全部跳过。
- "开始统计"（全员）与"单人统计"（`employeeIds` 批量）共用同一计算口径与已入库数据源，不在线调钉钉；无原始候选工作日的员工写零值占位行。
- 出勤时间手工保存：`/updateAttendanceSummary` 按 `employeeId+month` 批量覆盖三项小时并置 `attendance_manual_adjusted=1`，查询优先返回人工值。
- 考勤汇总：列表按部门 `deptIds`（真实组织）+ `department=1/2`（体系）双口径筛选；单元格白名单编辑保存；行政体系考勤 Excel 下载（`department=1` 强制，E/F/G 列与统计页同口径）。

## 设计与契约
- 明细表 `hrm_overtime_night_statistics_detail`：`employee_id + work_date` 唯一；重算按 `work_date` 整月范围删除（禁按 stat_year/month）；删除用 JPQL bulk delete；索引 `work_date` 与 `employee_id, work_date`；统计入口无长事务（`TransactionOperations` 只包删除+保存）；`calc_process` 列存当日计算过程（明细/汇总悬浮展示，SQL `2026-09-06_overtime_night_calc_process.sql`，全租户执行）。
- 排班事实源（2026-09-06 起）：只用 `tbplanlist`（按 `UserID` 逗号拆分匹配员工钉钉ID，缺失回退 `tbattendanceuser.UserID`），不再读 `hrm_attendance_plan`。班次时间解析：`shift_source=custom` 经 `custom_shift_id`→`hrm_workplan_custom_shift`（start1/end1/cross_day/shift_period/continuous_shift）→ 标准 `classId`→`hrm_attendance_shift` → date_shift 回退；`shift_source=rest`/调休不计加班；当日无 tbplanlist 记录即无排班、不计加班。
- 加班算法（2026-09-06）：有效工时=打卡分段累计（上/下班卡按序配对求和，白班"上午下班-下午上班"午休缺口天然排除）；仅一对上下班卡且排班非连班时再固定扣 2h 午休；首卡早于排班开始按排班开始计。加班=max(有效工时−8h, 0)，保留2位；本地审批加班单仍优先覆盖。
- 夜班算法（2026-09-06）：排班为夜班别（shift_period=night）或排班结束越过次日凌晨3点，且实际下班时间（缺卡回退排班结束时间）≥ 次日凌晨3点，计1个夜班（`OvertimeNightClockResolver.isNightShift/isScheduledNight`）。
- 审批优先：目标月存在本地加班审批时按审批时长日累计覆盖 `overtimeHours`；`statisticsStatus=取消至统计` 的审批在加班汇总、实际/应计出勤扣减中全部跳过。
- "开始统计"（全员）与"单人统计"（`employeeIds` 批量）共用同一计算口径与已入库数据源，不在线调钉钉；无原始候选工作日的员工写零值占位行（calc_process="零值占位行"）。
- 出勤时间手工保存：`/updateAttendanceSummary` 按 `employeeId+month` 批量覆盖三项小时并置 `attendance_manual_adjusted=1`，查询优先返回人工值。
- 考勤汇总 `hrm_produce_attendance`：同步 upsert（保留人工字段）；`overtime_pay = 加班小时 × salary_basic.overtime_pay`（默认 12）、`night_subsidy = 夜班次数 × subsidy`（默认 30）；列表 SQL 显式列别名（禁 `select *`）。
- 端点：`queryPageList / startStatistics / startStatisticsForEmployee / queryEmployeeMonthlyDetail / queryDailyDetailPageList / updateAttendanceSummary / downloadStatisticsTemplate`；`syncFromOvertimeNightStatistics / updateCell / downloadAdministrativeAttendance`。主列表行与每日明细行均返回 `calcProcess`；hr_web 明细弹窗/每日总览页"每天加班小时/每天夜班次数"面板的汇总列与日单元格悬浮显示（tooltip）。

## 近期变更
- 2026-09-07 `calc_process` 列漏执行修复：薪资导出读加班/夜班统计明细（实体含 `calcProcess`）时，租户库缺列报 `Unknown column 'calc_process'`；已在本地 6 个租户库（hr_0001~hr_0006，含脚本未覆盖的 hr_0006）补执行该列。**生产部署仍需执行 `../sql/2026-09-06_overtime_night_calc_process.sql`**（注意脚本只写了 hr_0001~hr_0005，新租户库需照 pattern 补）。
- 2026-09-07 行政体系考勤导出批注：`downloadAdministrativeAttendance` 为事假/病假/调休/年假/出差/旷工/迟到/早退/上下班缺卡/总缺卡/加班 11 列加单元格批注（发生日期 + 计算过程，全中文）。日期来源：请假/出差/加班取 `tbattendanceapprove.workDate`（按类型分组），旷工/迟到/早退/缺卡取 `hrm_attendance_report_data` 按天明细（新查询 `queryAdministrativeAttendanceReportDetail` + `AdministrativeAttendanceReportDetailVO`）；批注文本由 `AdministrativeAttendanceExportSupport` 静态方法生成、行对象 `columnComments` 承载。**连带修复**：`resolveAdministrativeAttendanceApprovalType` 原不返回"出差"，合并分支死代码致出差列恒 0，现补 `tagName/subType 含"出差"` 判定（出差列开始有值，时数÷8 折天）。测试 `HrmProduceAttendanceServiceImplTest` 补 `salaryConfigService` mock（此前缺 mock 致 5 个下载用例在 `resolveAdministrativePayDay` NPE，属既有欠账）。
- 2026-09-06 加班/夜班算法定版：①资格改"生产体系**或**固定月休"（OR）；②排班源切到 `tbplanlist`（钉钉排班快照弃用）；③加班按打卡分段累计有效工时（午休缺口天然排除；仅一对卡且非连班再扣2h），超8h计加班；④夜班=排班夜班别/结束过次日凌晨3点 **且** 下班时间≥次日凌晨3点；⑤明细新增 `calc_process` 计算过程列，前端明细弹窗/每日总览悬浮展示。改动 `HrmOvertimeNightStatisticsServiceImpl`、`OvertimeNightClockResolver`、明细实体/3个VO、`docs/sql/2026-09-06_overtime_night_calc_process.sql`（生产部署需全租户执行）、hr_web `Index.vue`/`DailyDetailPage.vue`/`overtime-night-utils.js`。重跑"开始统计"后生效。

## 历史摘要
- 2026-08-26 应出勤天数修复：`rest_type=2` 员工（含行政归属）不按行政单双休误算，改"当月总天数 − productionMonthlyRestDays"。
- 2026-08-10 出勤时间手工保存上线；2026-07-25 固定月休修复（restType 优先）+ 统计范围合并；2026-04~07 功能一期/落库设计/审批优先/口径收口/锁等待治理/考勤汇总同步（07-15/17）。
- 班次解析旧优先级（钉钉 classId/groupId/shiftSetting/date_shift 链）自 2026-09-06 起仅作 classId 缺失时的回退，排班事实源以 tbplanlist 为准。
- 全部细节见 `../archive/changelog.md` 与 `../archive/development-2026-08-30.bak.md`。
