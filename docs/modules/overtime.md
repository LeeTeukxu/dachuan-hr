# 加班/夜班统计 与 考勤汇总

菜单前缀：`/hrmOvertimeNightStatistics`、`/hrmProduceAttendance`（考勤汇总，同步方向固定"加班/夜班统计 → 考勤汇总"）。

## 需求要点
- 加班/夜班资格（全局口径）：仅 `affiliation_system=2`（生产体系）且 `rest_type=2`（固定月休4天）员工统计加班小时/加班费/夜班次数/夜班补贴；其余员工这些字段强制为 0，但应出勤/实际出勤/应计出勤链路保留。
- 应出勤班制按 `rest_type` 优先：`rest_type=2` 按"自然月天数 − 基本工资设置 `productionMonthlyRestDays`（默认 4，不另扣法定节假日）"；`rest_type=1` 按单双休月历 `workDays`；`rest_type` 为空才按 `affiliation_system` 兼容旧分流。薪资核算的应出勤唯一来源是本模块落库 `expected_attendance_days`，缺失即阻断并提示维护"单双休设置"。
- 实际出勤 = `应出勤小时 + 可计入加班小时 − 事假/病假/调休/年假扣减`；行政体系可计入加班仅本地审批加班（不含日级自动加班），其他体系用月度加班汇总；`外出/出差/补卡` 不扣减；展示加班 `overtimeHours` 与可计入加班 `attendanceOvertimeHours` 拆分。
- 应计出勤 = `实际出勤 + 调休 + 年假 − 加班 + 病假`（本地审批口径）。
- 审批优先：目标月存在本地加班审批时按审批时长日累计覆盖 `overtimeHours`；`statisticsStatus=取消至统计` 的审批在加班汇总、实际/应计出勤扣减中全部跳过。
- "开始统计"（全员）与"单人统计"（`employeeIds` 批量）共用同一计算口径与已入库数据源，不在线调钉钉；无原始候选工作日的员工写零值占位行。
- 出勤时间手工保存：`/updateAttendanceSummary` 按 `employeeId+month` 批量覆盖三项小时并置 `attendance_manual_adjusted=1`，查询优先返回人工值。
- 考勤汇总：列表按部门 `deptIds`（真实组织）+ `department=1/2`（体系）双口径筛选；单元格白名单编辑保存；行政体系考勤 Excel 下载（`department=1` 强制，E/F/G 列与统计页同口径）。

## 设计与契约
- 明细表 `hrm_overtime_night_statistics_detail`：`employee_id + work_date` 唯一；重算按 `work_date` 整月范围删除（禁按 stat_year/month）；删除用 JPQL bulk delete；索引 `work_date` 与 `employee_id, work_date`；统计入口无长事务（`TransactionOperations` 只包删除+保存）。
- 班次解析优先级：`classId→hrm_attendance_shift` → 历史 `hrm_attendance_history_shift` → `groupId（含 old_group_id）→ shiftSetting`（周一~周日索引）→ `hrm_attendance_date_shift` 最近快照；计划下班时间来源仅 `SHIFT` 可自动计加班，`PLAN_CHECK_TIME/CLOCK_ATTENDANCE` 回退不产加班（除非有既有加班记录）。
- 同日多计划选 `check_type=OffDuty` 且 `planCheckTime` 非空最大者；跨天班次次日下班卡并回原工作日；月末跨天禁生成次月脏明细。
- 考勤汇总 `hrm_produce_attendance`：同步 upsert（保留人工字段）；`overtime_pay = 加班小时 × salary_basic.overtime_pay`（默认 12）、`night_subsidy = 夜班次数 × subsidy`（默认 30）；列表 SQL 显式列别名（禁 `select *`）。
- 端点：`queryPageList / startStatistics / startStatisticsForEmployee / queryEmployeeMonthlyDetail / queryDailyDetailPageList / updateAttendanceSummary / downloadStatisticsTemplate`；`syncFromOvertimeNightStatistics / updateCell / downloadAdministrativeAttendance`。

## 近期变更
- 2026-08-26 应出勤天数修复：`rest_type=2` 员工（含行政归属）不再按行政单双休误算，改"当月总天数 − productionMonthlyRestDays"；薪资从明细表读取同步生效，需重新"开始统计"。
- 2026-08-10 出勤时间手工保存上线（三小时列 + 人工标记 + 查询优先 + 考勤汇总/行政导出同步读取）；SQL `2026-08-10_overtime_night_attendance_manual_hours.sql` 已在 5 租户库执行。
- 2026-07-25 固定月休修复（`restType` 优先于 `affiliationSystem`，李凤皇式 184→200 修正）+ 开始统计范围选择合并（删除独立"单人统计"按钮，复用 `employeeIds` 批量）；同日行政体系考勤汇总下载上线。

## 历史摘要
- 2026-04-04~09：功能一期（实时计算）→ 落库设计/实现（明细表）→ 每日总览页 → 既有加班记录兼容 → 唯一键冲突修复 → sync 口径收口 → 单人统计端到端与许泽刚专项（OffDuty 择优、date_shift 回退、月报/审批优先来源等十余轮排查，最终收敛为"与开始统计同口径"）。
- 2026-05-26：本地加班审批优先口径 + 零值占位行。
- 2026-06-03：固定导出 `jbtj.xlsx`。
- 2026-07-12~15：应出勤/实际出勤天数字段与扣减口径系列、应计出勤、年假扣减、行政/非行政加班分流、锁等待治理（bulk delete + 短事务）、考勤汇总同步上线（2026-07-15/17）。
- 全部细节见 `../archive/changelog.md` 与 `../archive/development-2026-08-30.bak.md`。
