# 薪资管理（薪资档案 / 月记录 / 核算与导出 / 个税与附加扣除 / 工资条）

菜单前缀：`/hrmSalary*`（管理/档案/基本工资）、`/hrmPersonalIncomeTax`、`/hrmAdditional`、`/hrmEmployeeAdditional`、`/hrmHolidayDeduction`。

## 需求要点
- 核算：仅一个"开始核算"入口（按人员/按部门树选择，展开 `employeeIds`，上限 50 人，同名按"姓名+手机号"识别，员工 ID 字符串保真）；真实进度接口（阶段 PREPARE/LOAD_DATA/CALCULATE/PERSIST/FINISH/ERROR）+ 失败问题集中明细展示；核算前置集中校验（缺工号/缺考勤/应出勤缺失等）返回可定位员工的中文错误。
- 计薪员工口径：`entry_status in (1,3)`，或 `entry_status=4` 且 `plan_quit_time > 薪资结束日 − 1 个月`；"同步考勤"穿梭框复用同一口径（`queryComputeSalaryEmployeeList`）。
- 应计出勤：工资项编码 2 与 `actual_work_day` 字段名保持兼容，口径为"应计出勤天数"；优先考勤汇总 `probation_attendance`（0 视为明确值），缺失回退加班/夜班统计 `accrued_attendance_hours/8`。应出勤唯一来源 `expected_attendance_days`，缺失阻断。
- 病假：≤2 天不扣病假工资（`19010401=0`）但扣全勤；>2 天按 `(salaryBasic/应出勤天数)×(天数−2)` 四舍五入到整数；行政/生产都按"小时/8"折算天数。全勤兜底：正式+启用全勤+无有效病假+应计出勤≥应出勤即发 `40102`（不因报表汇总存在缺卡/短迟到而拒绝）。
- 个税：`is_remark=2` 固定 60000 仅作缺可靠上月累计时的兜底，否则按"上月累计减除费用+5000"续算封顶 60000；本月个税 = `270106 − 250105`（下限 0）；核算后回写当前薪资月份 `hrm_personal_income_tax(end_month=当月)`，附加累计滚动生成次月；12 月不重置。
- 工会费 `160102` = 应发×0.5%（残疾员工不再豁免，免个税规则保留；2026-09-05 起取消"成都0002/攀枝花0005 整公司不收"的硬编码豁免，所有公司统一按规则收取）。
- 导出：满勤天数 = 应计出勤小时/8、超缺勤天数 = (应出勤天数×8−应计出勤小时)/8，均读统计落库值；支持按人员/部门范围选择（50 人上限）；异常必须传播为失败响应，禁空白 Blob。
- 基本工资金额设置（`hrm_salary_basic`，五项配置默认 100/500/4/15/3）：保存后同步更新未删除员工薪资档案 `10101`（`is_pro in (0,1)`），不自动建档；普通/领导全勤金额支持员工级覆盖（`ordinary/leader_full_attendance_amount`，员工表优先）。
- 薪资档案：在职（status=11）/离职（status=15）卡片筛选；单个定薪先取 salaryBasic 再渲染模板。月记录：新建次月按请求源年月定位且幂等复用；误建后续空月可"预览+恢复"（有明细/工资条/已发送则阻断）。

## 设计与契约
- 核心类：`SalaryMonthRecordServiceNew`（主流程/导出/恢复）、`SalaryComputeServiceNew`（单项计算/个税）、`SalaryMonthRecordService_Bak`（旧链路仍被工资条/任务引用，需同步防护）。锁规范：`COMPUTE_RECORD_LOCK_MAP` 锁对象常驻；核算/恢复/新建下月/审核确认"先取锁、锁内 `runInTransaction` 开事务"。
- 关键工资项：10101/10102/10103 基本岗位职务工资、180101 加班费、180102 夜班补贴、190103 旷工、19010401 病假、200101 超缺勤、210101 应发、230101 个税、240101 实发、250105 累计已缴、270101~270106 个税累计、40102 全勤奖、160102 工会费、41001 绩效/奖金。
- 并发禁令：非固定工资项初始化禁预插 `1001/160102/210101/220101/230101/240101/2501xx/2701xx/41001` 默认 0，只由正式核算生成。
- 导入模板下载：个税累计/附加累计/年度专项扣除三个固定模板接口；导入匹配 `TaxImportEmployeeMatcher`（姓名+手机号优先，重名缺手机号拒绝），文件内同年月/年度去重。
- 上传调薪/定薪（watch 迁移）：`/hrmSalaryArchives/importSalaryFixing|downloadSalaryFixingTemplate|downloadSalaryFixingData`。模板沿用 watch 格式（第 4 行起数据，姓名+手机号匹配员工，列 1-3 试用期、4-6 正式期的基本/岗位/职务工资，code 10101/10102/10103），模板文件 `export/调薪定薪导入模板.xlsx`；导入复用 `setFixSalaryRecord` 落三表，任一行校验失败整体不导入并返回逐行错误；下载数据导出全部员工档案（`queryEmpSalaryArchivesList`）。前端 `hr_web` 薪资档案页三按钮。

## 近期变更
- 2026-09-08 按部门递归含子部门：①薪资档案（`querySalaryArchivesList`/`queryEmpSalaryArchivesList`）、工资条（`querySlipEmployeePageList`）、薪资列表/月度导出（`SalaryMonthRecordServiceNew.fillDeptIdsWithChildren` 助手+3 调用点）、历史薪资明细（`queryHistorySalaryDetail`）均 service 层 `RecursionUtil.getChildList` 递归解析子部门后 IN 查询（`QuerySalaryArchivesListDto`/`QuerySlipEmployeePageListDto`/`QuerySalaryPageListDto`/`QueryHistorySalaryDetailDto` 增 `deptIds`，XML `<choose>` IN/otherwise 兜底原 `=`）；②仪表盘 `DashboardAggMapper.xml` 19 处 deptId 精确匹配改为以 `#{deptId}` 为根的 `WITH RECURSIVE dept_tree` IN 子查询（personnelOverview 6 项、structure、deptStructure、hireTrend、quitTrend、keyQuitTrendByDept、keyQuitCount、crossMatrix、quitDist、personnelPageList、flowPageList×2、salaryEmpDetail、perfTrend、perfCompletion），service 层零改动，未传 deptId 行为不变；选中部门后各图表统计含子部门（deptStructure 选中父部门返回该部门及子部门多行）。
- 2026-09-07 导出批注全中文化+扩展：`exportSalaryNew` 按员工读取工资项明细（190101 迟到/190102 早退/190103 旷工/19010401 病假/19010402 事假/190105 缺卡、270101~270106+250105 个税累计、281 其他补贴），生成 5 列批注（其他补贴 16、全勤奖 17、超缺勤 19、个税 21、工会费 25）写入 `HrmSalaryExport` transient 字段；`SalaryExportCommentWriteHandler` 优先取预生成文本、缺失回退通用文案，批注禁出现工资项编号等术语。
- 2026-09-06 上传调薪/定薪三件套（watch 迁移）：档案页新增"上传调薪/定薪/下载模板/下载数据"，Excel 格式沿用 watch，导入走既有 `setFixSalaryRecord`，校验失败整批拒绝并返回逐行错误。

## 历史摘要
- 2026-09-06 扣款规则"默认标记优先"：`selectEffectiveAttendanceRule` 优先取 `is_default_setting=1` 规则，保存默认规则互斥清理其他标记；前端考勤规则页新增"设为默认规则"开关。
- 2026-09-06 早退扣款上不封顶（移除误用迟到单价的封顶分支，迟到封顶不变）。
- 2026-09-05~03：工会费取消成都/攀枝花公司级硬编码豁免、计薪设置管理界面 `SalaryConfig.vue`、新租户薪资界面 NPE 修复与 `hrm_salary_config` 种子数据。
- 2026-07-16~22：开始核算范围选择（50 人上限、部门树、规则说明）、导出范围、附加累计按年-月筛选、个税备注口径、个税/附加导入重名修复与模板下载、固定月休/全勤/体系口径（`rest_type` 收紧加班/夜班资格）、基本工资设置五项配置与档案同步、员工级全勤金额；多轮数据排查（吴镜平、采购计划部负数超缺勤、李明明/张明全勤与个税、农谷补贴/档案/社保整理）详见 changelog。
- 2026-07-20：应出勤天数清理（禁 21.75 兜底）+ 满勤/超缺勤导出口径修正 + 导出空白修复。
- 2026-07-15：考勤汇总同步加班/夜班统计（见 overtime.md）；2026-06-18：只计税奖金进入累计收入（见 bonus.md）；2026-04-02：核算真实进度。更早细节见 `../archive/changelog.md`。
