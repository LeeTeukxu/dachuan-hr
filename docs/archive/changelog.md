# Changelog（历史轮次归档）

> 本文件收纳模块文档"近期变更"之外被压缩的历史轮次，按模块分节、保留日期标题，是唯一的细节追溯处（模块文档每份 ≤150 行，只保留当前生效行为与最近 3 轮）。
> 更完整的原文：`development-2026-08-30.bak.md`（原 development.md 全文 3073 行）、`requirements-2026-08-30.bak.md`（原 requirements.md 需求条目全文）、`考勤同步并发风险与优化方案-2026-08-30.md`。

## 系统管理（system.md）
- 2026-05-08 登录"账号不存在"排查：`hrsystem.tbAllUserList` 有账号但租户 `tbloginuser` 缺失，或 `view_loginuser`（tbloginuser×hrm_dept×tbroletypes 内连接）任一关联缺失，均表现"账号不存在"；诊断信息当时未拆分。
- 2026-06-16 权限管理与登录用户创建：`MenuPermissionSupport`/`ApiPermissionPathSupport` 上线；登录成功加载 `rolemenu/menuTree` 写入 token；登录加载菜单需临时设置 `CompanyContext`（拦截器跳过登录路径时 JPA 否则落总库报 `could not extract ResultSet`）；`tbrolemenu.role_menu_id` JPA 主键改 Long；`TbRoleTypes.canUse` 显式驼峰列；登录用户保存校验部门/角色/权限，系统库账号索引独立事务 + 删旧插新；菜单种子 SQL 只补 path 与缺失菜单、不自动扩权。
- 2026-06-17 系统设置权限可见性修复：父级菜单不能单独授权；`TbMenuService#queryMenuList` 的 Integer 引用比较改 `Objects.equals`；hbadmin（roleId=2）补授 `/hrm/system/*` 与 `/manage/*` 子菜单 SQL。
- 2026-06-17 假期扣减 500 修复：`hrm_holiday_deduction.deduction_id` 为 varchar(64) UUID，BO/VO 改 String。
- 2026-06-17 远端发布链路确认：公网 nginx 代理的远端 9080 需手工替换 JAR 并重启，本机打包不影响公网。
- 2026-06-17 Tomcat 请求头 400：管理员 token 约 7.9KB + 浏览器头超默认 8KB；三个 profile 增加 `server.max-http-header-size=65536`。
- 2026-06-17 登录用户默认部门与删除：新建缺省部门取顶级部门；`Delete` 同时删租户用户与系统库账号索引（`runWithDefaultCompanyContext` 切默认数据源）。
- 2026-08-21 `cfy` 登录修复（详见 system.md 近期变更）；现场重复索引仅 `cfy` 一例，发布后编辑/保存该账号会顺带清理。

## 员工管理（employee.md）
- 2026-06-10：批量设置参保方案（`/hrmEmployee/updateInsuranceScheme` 事务整体回滚 + 空列表/缺方案校验）；员工编辑部门/学历保存修复（学历空指针、雪花 ID 字符串兼容）。
- 2026-06-11：员工年龄/司龄口径排查（age 导入写入不自动刷新、司龄实时计算）；列表固定字段驼峰别名修复（Map 结果无下划线转驼峰包装器）；身份证兜底生日/年龄、动态字段空 key 防序列化失败；生日 `MM-dd` 与司龄链路（岗位详情刷新、新增默认入职日期、`computeCompanyAge(null)` 返回空串）。
- 2026-06-12：高级查询增强（年龄/出生月份/司龄/合同结束/签订次数/用工性质/政治面貌，转正日期范围需首尾完整）；合同新增不覆盖（`addContract` 清空 contractId）与期限自动计算；组织部门编码自动生成（`generateCode` 最小未用正整数，保存时覆盖）；花名册字段比对（报告 `docs/employee-roster-field-comparison-2026-06-12.md`）。
- 2026-06-13：子女信息（`children_info` fieldId=900001、detail_table 动态字段）；线上字段配置幂等补齐 SQL；保存 JSON 解析修复（fieldValueDesc 不能传数组）；组织编码接口 404 前端兜底。
- 2026-06-14：基础信息模板导出（24 字段全来源映射、合同列口径、动态字段驼峰与蛇形兼容）。
- 2026-06-16：合同导入（姓名+电话匹配、合同类型/状态枚举与多格式日期解析、整体回滚、`2027-4-31` 误报空修复）；固定模板下载（花名册/合同）。
- 2026-07-17：唯一性校验统一（validateEmployeeUniqueFields 覆盖新增/导入/转正/调岗/通讯保存）+ 删除"是否有全勤/是否加入钉钉"表单。
- 2026-08-19/20：部门明细导出多轮结构补齐（顶部统计区、成本区、人员总表、合同到期日、数据对账 92 人全对）；员工新增/编辑薪资字段（三个动态字段幂等补齐、转正/调岗/晋升弹窗接入）；花名册模板运行时插列（固定绩效/职务补助，父表头合并处理）。
- 2026-08-21：其他补助第 4 个薪资动态字段 + 导入"姓名+身份证号"兜底；合同无固定期限（保存/导入清空 endTime/term、导出显示"无固定期限"、到期提醒不统计）。

## 排班与单双休（attendance-scheduling.md）
- 2026-04-06：每周单双休生成上线（`modules/workweek`、`hrm_workweek_setting` 建表脚本、单/双休交替、修改向后重算）。
- 2026-04-12 自定义班次设计（已废弃）：原设计将自定义班次解析为钉钉 shiftId 并调组同步；后被"仅本地保存"替代。
- 2026-04-12 自定义班次后端实现：saveAll 任务化（taskId/内存进度）、`hrm_workplan_custom_shift` 事实表、queryCustomShiftList、提交 10 分钟/展示 30 分钟缓存拆分、最终解析结果合并落库（同组同班次合并 UserID）、失败重试 10 次中文按行输出。
- 2026-04-12~13 后端契约修复：saveAll 500 根因（groupId 缺失兜底 enrichAndValidatePlan）、changeGroup 请求对象误用修复、loadIsLast 补 ID 与"同 id 同日期才更新"、多格式日期解析、调组缓存清理。
- 2026-05-30/31：`custom_shift_period` 白/夜班别；Excel 导入口径确认（全为本地自定义班次、时间写法 8:00-结束 需空结束时间支持）；排班管理只读记录→日历矩阵→员工日期矩阵→全员显示→设置入口迁移（打卡概况弹窗迁到排班管理）→关键字筛选→矩阵导出。
- 2026-06-01~04：调休实现（shiftType=rest 不推钉钉）；上传与模板下载迁移到排班管理（模板按 templateMonth 重写日期行、31 号清列）；导入模板支持调休；桌面排班.xlsx 适配预检与转换；上传直提与"同日同名手机号"规则（后被 2026-07-25 电话规则取代）；李冬 5 月 2 日未显示（URL 64KB → FormData）与同日两条排班（persistPlans 按 WorkDate+UserID 复用去重）；休息/调休/连班（`rest_shift_type`、`custom_continuous_shift`、唯一索引调整）；横向 workplan.xls 解析；ResultSet 修复（rest_shift_type 半迁移补列）。
- 2026-06-03/17：单双休后续变化（recalculateFollowing、月度统计、法定节假日覆盖）与月度日历（queryMonthCalendar/saveMonthCalendar 增量 upsert 修复唯一键冲突、holidayName、2026 国务院放假内置兜底 33 天法定休 + 6 天调休上班）。
- 2026-07-25：连班自动带出（`is_continuous_shift` 字段、批量按员工拆分、显式值保护 customContinuousShiftExplicit）+ 导入电话匹配规则（"姓名+电话"或纯姓名）。
- 2026-08-17：单日排班删除（共享行只移除目标员工）；白班未勾连班显式保存。
- 2026-08-22：生产产品/岗位/员工配置上线（三表 + /workPlanProduct 全套接口 + 菜单权限 SQL + 排序自动生成/拖拽持久化/岗位员工替换语义）。

## 考勤同步（attendance-sync.md）
- 2026-03-27：多租户打卡缺失专项（移除 create_time 拦截；并行流竞态、clock_stage 误过滤、高频回查超时三项修复）。
- 2026-04-03/04：同步实时进度接口（getSyncProgress）与空参校验；计薪口径员工查询接口（queryComputeSalaryEmployeeList）；Communications link failure 修复（DataSourcePoolConfigurator + 有限重试）；考勤同步漏人专项（禁 DeleteRepeatUser、normalizeUsers、失效映射按钉钉可见性清理 + hr_0003 精确清理 SQL）。
- 2026-04-09：sync/syncAll 入库链路梳理（7 步与直写表清单）；历史班次缺失根因（sync 只保最新快照，不保历史版本——业务确认接受）；最新考勤组班次补齐（shift_setting 写入、班次同步脱离部门关系）。
- 2026-04-13：钉钉月额度超限不污染本地快照（先拉全量成功再替换）；添加排班人员列表偶发空白（展示链路优先本地快照）；标准班次下拉垃圾数据收敛（type=TURN + 完整时段 + 与本地快照求交集）。
- 2026-05-08：审批数据功能上线（本地快照查询链路、无钉钉依赖）；"待选员工 99 人"口径确认（计薪员工 ≠ 员工列表）；审批入库缺口确认（sync 主链路不含审批步骤）。
- 2026-05-09~15：手工获取审批数据全链路（process.listbyuserid/listids/get、qyapi_aflow 权限错误翻译、业务时间月内过滤、完成标记表 hrm_attendance_approval_fetch_mark 兼容旧表升级、重抓清理（先删旧→后清理陈旧策略、同名员工保护、@Transactional 边界、@Modifying 返回类型 int 热修）、时间列去 ON UPDATE CURRENT_TIMESTAMP、日期区间组件/加班字段别名解析、审批类型宽匹配、员工 ID 精度串人修复、调用量最小化（日期级批量排班、50 人批量明细、任务级流程编码缓存、审批全员映射去重）。
- 2026-05-13：钉钉付费 API 调用量异常排查（步骤 3/4/7 放大估算 4371+705+1269+423 次；postresultlog 非完整台账；scheduling.enabled=false 说明）。
- 2026-06-01/05-31：打卡记录查看链路确认（本地快照、2026-04 历史月 create_time 拦截修复）；打卡概况多次打卡时间线（2026-04-16）。
- 2026-07-13~15：审批数据统计参与状态（statisticsStatus）；审批抓取钉钉用户不存在处理（员工表解析顺序、400023 跳过、多映射取最新）；行内添加员工审批（employeeId/mobile 返回、手工时长分摊）；2026-08-12 排班管理考勤信息改读同步数据（展示查询全面本地化）。
- 2026-08-16：顶栏钉钉 API 调用量展示（postresultlog 月度聚合 + 功能分布 + 80% 阈值）。

## 加班/夜班统计 与 考勤汇总（overtime.md）
- 2026-04-04：功能一期（实时计算、OvertimeNightClockResolver 下班打卡兜底）；2026-04-05：落库设计/实现（明细表 employee_id+work_date 唯一）；2026-04-05：明细总览页；2026-05-08：员工月度明细按月份过滤。
- 2026-04-07：兼容既有加班记录（hrm_employee_over_time_record 回填）；重算唯一键冲突修复（按 work_date 整月删除）；后端专项测试链路恢复（SalaryMonthRecordServiceNewTest 签名对齐）。
- 2026-04-08~09（许泽刚专项，约 12 轮）：OffDuty 排班兜底 → 空计划时间择优 → 单人统计返回值去旧值 → 按 Sheet3 收敛 fallback 口径 → 既有记录 attendanceTime 为空回填 → 改按 attendance_report_data 对齐 → 补审批加班来源（SingleEmployeeOvertimeHoursProvider）→ 回退纯入库统计（后被 2026-07-15 取代）→ 班次解析回退考勤组（MON..SUN 索引）→ 兼容历史组 ID（old_group_id）与历史班次表 → 诊断日志 [daily-resolution] → 回退日级班次快照（date_shift）→ 复用最近快照 → 主流程接入班次恢复（禁用 OnDuty planCheckTime 当上班时间）；调试日志 logs/hainan-overtime-debug.log。
- 2026-04-16：跨天班次夜班次数未计入修复（次日下班卡并回原工作日 + 已消费标记 + 月末边界禁次月脏明细）。
- 2026-05-26：本地钉钉加班审批优先口径（bizType=1/名称宽匹配、按 userId 严格隔离、日累计覆盖）+ 零值占位行（无候选工作日员工保留）。
- 2026-06-03：固定导出 jbtj.xlsx。
- 2026-07-12：应出勤/实际出勤天数字段（affiliation_system 接入、字段 DDL、公网前后端未发布复核）；实际出勤矩阵展示（替代出勤概览区域）；张明 160 小时来源排查。
- 2026-07-13/14：实际出勤扣减口径（应出勤+加班−事假/病假/调休）、审批时长内联修改、子类型内联修改、展示加班与实际出勤加班拆分（张明 47.72 日级自动加班排除）、年假扣减、李明明/闫倩校准（durationInHour、0.07 天纠偏 0.50h、通用审批误判修复）、应计出勤（accrued_attendance_hours DDL）、统计重算锁等待治理（取消长事务、bulk delete、work_date/employee_id+work_date 索引）。
- 2026-07-15：生产体系实际出勤加班来源修正（与行政一致只取审批加班）+ 2026-06 行政体系 Excel 对账（27 人差异分类）+ 王芳钉钉映射对调复核（通讯录校验刷新 dingtalk_user_id）+ 生产月休应出勤（30−4−1=25 天）。
- 2026-07-15/17：考勤汇总同步加班/夜班统计（syncFromOvertimeNightStatistics、overtime_pay 字段、潘红琼 department 刷新、mapper select * 显式别名）。
- 2026-07-23：固定月休应出勤修复（restType 优先）+ 开始统计范围选择前端合并。

## 薪资管理（salary.md）
- 2026-04-02：核算真实进度（queryComputeProgress，按租户+记录+范围隔离）；6012 社保校验放宽（有 status=1 员工明细即放行）；部门查询 JPA VO 转换异常修复。
- 2026-04-04：个税/附加列表年月倒序 + 前端年月列。
- 2026-06-18：只计税奖金进入累计收入（见 bonus.md）。
- 2026-07-15：考勤汇总同步加班/夜班统计的薪资侧（180101/180102 资格防护）。
- 2026-07-16：开始核算范围选择（50 人上限、部门树、ruleTip 短句分段）；基本工资设置新增全勤/生产月休配置；附加累计按年-月筛选；个税备注口径（is_remark=2 固定 60000，后被 2026-07-17 续算口径部分修订）；个税/附加导入重名匹配修复（TaxImportEmployeeMatcher + 王芳数据修复 SQL + 年度附加重复清理）。
- 2026-07-17：个税备注续算口径（上月累计+5000 封顶 60000）；李明明工会费预插 0 修复（filterNoFixedSalaryOptions）；薪资核算失败集中提示（errors 明细）；社保报表真实进度；个税计算基础数据整理（申报表口径、身份证匹配、final_import_excels）。
- 2026-07-17~21 排查专项（只读）：李明明全勤奖未计（兜底判定起源）；吴镜平离职计薪口径确认；采购计划部负数超缺勤（21.75 默认与 23 天混算根因，促成应出勤清理）；张明及 12 人全勤奖漏发（兜底放宽 + 病假排除）；张雪梅病假折算（小时/8）；李明明个税 2.40 支持导入数据（2026-05 个税累计 69859/30000/5749/246.90 + 附加 25657.50，批量 97 人）。
- 2026-07-20：薪资应出勤清理（只读统计表、禁 21.75/attendance_info 兜底）；导出满勤/超缺勤口径修正；导出空白修复（异常传播）；导出范围选择。
- 2026-07-21/22：员工所属体系影响面与硬编码清理（dept_type 移除、isProductionAffiliationSystem、rest_type 接入与加班/夜班资格收紧 180101/180102）；员工级全勤金额；基本工资同步薪资档案 10101；病假扣款规则确认（≤2 天免扣、扣全勤）。
- 2026-07-23：个税/附加模板下载；固定月休应出勤修复联动；加班/夜班开始统计范围合并。
- 2026-08-20：薪资档案在职/离职卡片筛选（status=11/15）。
- 2026-08-30 性能第一批（薪资部分）：COMPUTE_RECORD_LOCK_MAP 锁对象常驻、runInTransaction"先锁后事务"。

## 奖金中心（bonus.md）
- 无被压缩轮次；模块自 2026-06-18 上线，三轮详情见模块文档。

## 社保管理（insurance.md）
- 2026-06-18：社保详情/加班总览隐藏页 checkPath 权限与多租户补权 SQL；主列表高级筛选（times/deptIds、默认当前年、(year*100+month) 过滤）。
- 2026-06-19：删除人员筛选项；部门批量参保前端。
- 2026-07-16/17：基本工资设置新增大额医疗 15/长期护理 3 固定金额并进入方案与月度合计（已废弃口径）；社保报表真实进度。
- 2026-07-17/18 农谷数据整理：7 月社保/公积金表方案落库（85 人）与 6 月工资表重整（82 人），含回滚 SQL 与异常清单（马国华/谢杰杰/程传祥同名未自动更新）。
- 2026-07-20 薪资个人社保多 3 元排查：固定金额叠加 + 月记录早于方案重整的时序根因（黎冬霜专项）；结论"修正月记录后重算，不在导出层减 3"。
- 2026-08-22 一键设置/长期护理详情见模块文档（含 hr_0003 88 条脏数据 SQL 修正与 cdadmin/hr_0002 验证）。

## 数据与运行配置（dataconfig.md）
- 2026-04-03：连接池统一配置（DataSourcePoolConfigurator）与同步链路连接异常重试。
- 2026-04-09：dev 日志落文件 logs/hainan-dev.log。
- 2026-05-13：动态数据源 allowPublicKeyRetrieval 统一拼接；tbCompanyList 5 条租户连接串密码更新（tianyegufen→hainandachuan）。
- 2026-06-13：Maven clean package BOOT-INF/lib 反斜杠路径修复；打包模板资源恢复（jbtj.xlsx、员工基础信息模板入库）。
- 2026-08-17：薪资导出批注/工会费（见 salary.md）；Maven package 默认 profile 守卫恢复。
- 2026-08-18：服务器部署跨域修复（CrossDomainFilter、OPTIONS 放行、局部 @CrossOrigin 清理、线上 OPTIONS/POST 核验）；prod 一键打包脚本（package-prod.sh/.command）；Maven 默认 profile 回归。
- 2026-08-30 性能审查（完整清单，含跨模块项）：死循环快速失败与事务收缩（AttendanceUserManager/AttendanceGroupManager）；单例共享 users 清除（7 个类）；SimpleDateFormat ThreadLocal（10+ 处）；同步防重入 Redis 锁；线程池租户校验前置；薪资锁规范；社保 tryLock；动态调度修复；AttendanceDbLock 按 companyId；Redis TTL（排班元数据 30 天、ClassList 30 分钟）与 mSet 修复；FTP/附件/zip4j/ExcelWriter 资源释放；DingTalkLogRetentionCleanupTask；getOvertTime 复用客户端+100ms；FETCH_PROGRESS_MAP 过期清理；进度类字段 volatile；prod mapper 日志降级。全量 634 项测试回到 16 个存量失败基线（ApplicationProfileConfigTest×1、合同导入×6、LoginControllerTest×3、加班统计断言×5、司龄 mock×1）。

## 文档与流程类（已并入 development.md/PRD）
- 2026-04-19/20：新增 PRD.md；新增钉钉接口及数据库 .md/.xlsx 与生成脚本。
- 2026-05-31 打卡记录 proxy request failed 排查（本地代理链路，前端配套 formatProxyRequestError）。
- 2026-06-16 远端 404 类排查（exportBasicInfoTemplate 旧包、generateCode 404 兼容）属发布流程记录，处置规则已并入对应模块文档。
