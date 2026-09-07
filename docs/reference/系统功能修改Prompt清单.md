# 系统功能修改 Prompt 清单

> 用途:按下方基准 Prompt,把整个系统的功能整理成"可直接复制使用"的修改请求。每个功能给出:①可直接复制的 Prompt(含业务描述与相关数据库表);②参考文件清单(Controller / Service / Mapper / Entity / Vue,含前端 API 层)。
>
> 代码根目录(下文文件路径均从对应根目录起写):
> - 后端根:`/Users/jiangyongming/Project/hr_copy/hainan`
> - 前端根:`/Users/jiangyongming/Project/hr_copy/hr_web`
>
> 基准 Prompt 模板:
> 这是 SpringBoot 多租户 SaaS 系统,我要修改【xxx 业务,描述需求】,相关数据库表:table_name。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。
>
> 通用说明:本项目数据访问层同时存在 MyBatis Mapper(`src/main/resources/mapper/*.xml` + `src/main/java/.../mapper/*Mapper.java`)与 JPA Repository(`src/main/java/.../repository/*.java`),涉及数据访问时两者都可能是改动点;多租户数据按 `CompanyContext` 自动路由到 `hr_0001…hr_000X` 各租户库,系统级表在 `hrsystem` 主库。

---

## 一、登录认证与账号体系

### 1.1 登录 / 验证码 / 一号多企业确认 / 个人信息「切换企业」/ 修改密码 / 退出登录
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【登录与账号认证:账号密码登录、图形验证码、一号多企业选择确认、个人信息页登录态内「切换企业」(POST /hrsystem/switchCompany 换发绑目标企业 token + /switchCompany/candidates 候选列表)、修改密码、退出登录、首次登录强制改密、账号锁定与会话失效】,相关数据库表:tbloginuser、tbAllUserList、tbcompanylist。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/LoginController.java`(含 `switchCompany`/`switchCompanyCandidates`)、`src/main/java/com/tianye/hrsystem/controller/CaptchaController.java`
- Service/组件:`src/main/java/com/tianye/hrsystem/common/JWTTokenUtils.java`、`src/main/java/com/tianye/hrsystem/common/TokenRevocationService.java`、`src/main/java/com/tianye/hrsystem/config/CompanyInterceptor.java`、`src/main/java/com/tianye/hrsystem/config/CompanyContext.java`
- Mapper:`src/main/java/com/tianye/hrsystem/mapper/LoginUserMapper.java`
- Entity:`src/main/java/com/tianye/hrsystem/model/LoginUserInfo.java`、`src/main/java/com/tianye/hrsystem/model/tbloginuser.java`
- Vue:`src/views/Login.vue`、`src/api/login/user.js`、`src/components/RestPassword.vue`、`src/views/hrm/profile/Index.vue`(切换企业按钮+弹窗)、`src/utils/authSession.js`、`src/utils/permission.js`、`src/router/config.js`、`src/router/router.js`、`src/api/requset.js`

### 1.2 登录用户管理(权限管理-登录用户)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【登录用户管理:登录账号的增删改查、重置密码、启用禁用、按部门/角色分配】,相关数据库表:tbloginuser、tbAllUserList、tbroletypes、tbrole_menu。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/loginuser/controller/TbLoginUserController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/loginuser/service/`(TbLoginUserService 等)
- Mapper:`src/main/resources/mapper/TbLoginUserMapper.xml` 及对应 `src/main/java/com/tianye/hrsystem/mapper/LoginUserMapper.java`
- Entity:`src/main/java/com/tianye/hrsystem/modules/loginuser/entity/TbLoginUser.java`、BO/VO 同目录
- Vue:`src/views/hrm/system/LoginUser.vue`、`src/views/hrm/system/login-user-utils.js`、`src/api/hrm/system/permission.js`

### 1.3 角色权限 / 菜单权限
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【角色权限分配与菜单权限清单:角色-菜单授权、菜单维护、权限树、批量角色同步】,相关数据库表:tbroletypes、tbrolemenu、tbmenu、tb_api_permission。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/role/controller/TbRoleTypesController.java`、`src/main/java/com/tianye/hrsystem/modules/menu/controller/TbRoleMenuController.java`、`src/main/java/com/tianye/hrsystem/modules/menu/controller/TbMenuController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/MenuServiceImpl.java`、`src/main/java/com/tianye/hrsystem/modules/menu/service/ApiPermissionPathSupport.java`、`src/main/java/com/tianye/hrsystem/modules/menu/service/TbRoleMenuService.java`（含批量角色同步逻辑）
- Mapper:`src/main/resources/mapper/TbRoleTypesMapper.xml`、`src/main/resources/mapper/TbMenuMapper.xml`、`src/main/java/com/tianye/hrsystem/mapper/LoginUserMapper.java`（含 `getAccountsByRoleId`、`getAccountCompanies`）
- BO:`src/main/java/com/tianye/hrsystem/modules/menu/bo/QueryRoleMenuBO.java`（含 `syncToOtherCompanies`、`targetCompanyIds`）
- Vue:`src/views/hrm/system/RolePermission.vue`（含企业选择弹窗）、`src/views/hrm/system/MenuList.vue`、`src/api/hrm/system/permission.js`（含 `getCompanies`）

### 1.4 租户管理 / 企业权限 / 企业开通
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【租户管理与企业权限:新租户开通建库、企业列表、企业权限分配、钉钉应用配置(ddAccount)】,相关数据库表:tbcompanylist、tbAllUserList、tbloginuser、ddAccount。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/company/controller/TenantProvisionController.java`、`src/main/java/com/tianye/hrsystem/modules/company/controller/TbCompanyListController.java`、`src/main/java/com/tianye/hrsystem/controller/CompanyPermissionController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/company/service/TenantProvisionService.java`
- Entity:`src/main/java/com/tianye/hrsystem/modules/company/entity/TbCompanyList.java`、`src/main/java/com/tianye/hrsystem/entity/po/ddAccount.java`
- 多租户基础设施:`src/main/java/com/tianye/hrsystem/config/CompanyDataSourceProvider.java`、`CompanyIdentifierResolver.java`、`DynamicDataSource.java`、`ConnectionParsor.java`
- Vue:`src/views/hrm/system/TenantManage.vue`、`src/views/hrm/system/CompanyPermission.vue`

## 二、数据统计

### 2.1 数据统计看板(首页)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【数据统计看板:首页人员信息/薪酬成本/绩效考核指标/入离职分析等图表与集团汇总】,相关数据库表:hrm_employee、hrm_salary_month_emp_record、hrm_performance_indicator、hrm_dashboard_role_permission(聚合读各业务表)。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/dashboard/controller/DashboardController.java`(含看板权限)
- Service:`src/main/java/com/tianye/hrsystem/modules/dashboard/service/imple/DashboardServiceImpl.java`
- Mapper:`src/main/resources/mapper/DashboardAggMapper.xml`
- Vue:`src/views/hrm/home/Blank.vue`、`src/api/hrm/home/dashboard.js`、`src/views/hrm/system/DashboardPermission.vue`

### 2.2 报表(旧版统计报表)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【旧版统计报表:报表数据查询与导出】,相关数据库表:hrm_attendance_report_data、hrm_attendance_report_field、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmReportController.java`
- Mapper:`src/main/resources/mapper/HrmAttendanceReportDataMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmAttendanceReportData.java`、`HrmAttendanceReportField.java`

## 三、组织与员工

### 3.1 组织管理(部门)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【组织管理:部门树的增删改查、部门明细、部门人数统计】,相关数据库表:hrm_dept、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmDeptController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmDeptServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmDeptMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmDept.java`
- Vue:`src/views/hrm/dept/Dept.vue`、`src/api/hrm/dept/dept.js`

**分管领导联动(2026-09-07):**保存部门(`/hrmDept/addDept|setDept`)设置分管领导时,部门下全部未删除员工直属上级(`hrm_employee.parent_id`)同步改为该分管领导;新端点 `/hrmDept/queryDeptLeader/{deptId}` 供员工表单选部门后自动带出直属上级(前端 `AddOrEdit.vue`/`DepAddEmployeeDialog.vue`)。

**同步钉钉数据(watch 迁移,2026-09-06):**`/hrmDept/syncDingTalkDept|getExcludeNames|saveExcludeNames`,核心 `imple/HrmDeptDingTalkSyncService.java`(listsub 递归+按名称 upsert+归属以钉钉为准覆盖+全量模式完全覆盖删除多余部门/分公司模式删关键字命中部门+员工自动转根部门+同名未占用行认领保证幂等+拉取失败重试3次),配置表 `hrm_dept_sync_config`,凭据 `imple/ddTalk/DDAccessToken.java`。

### 3.2 员工管理(花名册/列表/新增编辑/导入导出/动态字段)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【员工管理:员工花名册列表与筛选、员工新增/编辑/再次入职/办理离职/转正/调岗、花名册导入导出、部门明细导出、员工自定义动态字段】,相关数据库表:hrm_employee、hrm_employee_data、hrm_employee_field、hrm_employee_field_manage、hrm_employee_field_config、hrm_employee_change_record、hrm_employee_abnormal_change_record、hrm_employee_quit_info、hrm_key_post_config。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmEmployeeController.java`、`src/main/java/com/tianye/hrsystem/controller/HrmEmployeeFileController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeServiceImpl.java`、`src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeeFieldServiceImpl.java`、`src/main/java/com/tianye/hrsystem/imple/employee/EmployeeBasicInfoExportSupport.java`、`EmployeeDepartmentDetailExportSupport.java`、`EmployeeDepartmentDetailCrossCompanyExportSupport.java`(同目录)
- Mapper:`src/main/resources/mapper/HrmEmployeeMapper.xml`、`HrmEmployeeDataMapper.xml`、`HrmEmployeeFieldMapper.xml`、`HrmEmployeeFieldManageMapper.xml`、`HrmEmployeeFieldConfigMapper.xml`、`HrmEmployeeChangeRecordMapper.xml`、`HrmEmployeeAbnormalChangeRecordMapper.xml`、`HrmEmployeeQuitInfoMapper.xml`、`HrmKeyPostConfigMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmEmployee.java` 及同目录 HrmEmployeeData/Field/ChangeRecord/QuitInfo 等
- Service 补充:`src/main/java/com/tianye/hrsystem/imple/employee/HrmEmployeePostServiceImpl.java`(岗位信息编辑,部门变更自动改直属上级为分管领导)
- Vue:`src/views/hrm/employee/Index.vue` 及 `src/views/hrm/employee/Components/`(含 `attendance-sync-progress-utils.js`、`AddOrEdit.vue`、`DepAddEmployeeDialog.vue`)、`src/api/hrm/employee/employee.js`

**批量设置与同步钉钉员工(watch 迁移,2026-09-06):**`/hrmEmployee/listForBatchSetting|batchSetting/save|syncDingTalkRoster`,核心 `imple/employee/HrmEmployeeDingTalkSyncService.java`(userid 绑定+手机号优先+姓名唯一兜底+预检 dryRun,冲突只报告,姓名/手机号/部门/工号不改仅预检提示,源头字段岗位/邮箱/入职日期/工作地点以钉钉覆盖,拉取失败重试3次),批量设置字段白名单在 `HrmEmployeeServiceImpl` switch 映射;需 DDL `docs/sql/2026-09-06_dingtalk_sync_and_batch_setting.sql`。

### 3.3 员工合同
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【员工合同:合同新增/编辑/导入/到期提醒/无固定期限合同处理】,相关数据库表:hrm_employee_contract、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmEmployeeContractController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmEmployeeContractServiceImpl.java`(impl 在 `imple/employee/` 亦有测试对应实现)
- Mapper:`src/main/resources/mapper/HrmEmployeeContractMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmEmployeeContract.java`
- Vue:`src/views/hrm/employee/`(合同组件位于员工管理 Components 内)、`src/api/hrm/employee/employeeContract.js`

### 3.4 员工岗位 / 招聘渠道 / 招聘候选人
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【员工岗位与招聘:岗位维护、招聘渠道、候选人记录】,相关数据库表:hrm_employee_post、hrm_recruit_channel、hrm_employee_candidate。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmEmployeePostController.java`、`src/main/java/com/tianye/hrsystem/controller/HrmRecruitChannelController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmRecruitCandidateServiceImpl.java`、`HrmRecruitPostServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmEmployeePostMapper.xml`、`HrmEmployeeCandidateMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/`(HrmEmployeePost、HrmRecruitChannel、HrmEmployeeCandidate)
- Vue:`src/views/hrm/employee/`、`src/api/hrm/employee/employeePost.js`

### 3.5 员工社保信息(档案)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【员工社保信息:员工个人参保方案维护与查询】,相关数据库表:hrm_employee_social_security_info、hrm_insurance_scheme。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmEmployeeSocialSecurityController.java`
- Mapper:`src/main/resources/mapper/HrmEmployeeSocialSecurityMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmEmployeeSocialSecurity(Info).java`
- Vue:`src/views/hrm/employee/`、`src/api/hrm/employee/employeeSocialSecurity.js`

### 3.6 员工请假 / 加班记录(审批落库记录)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【员工请假与加班记录:请假/加班台账查询与维护】,相关数据库表:hrm_employee_leave_record、hrm_employee_over_time_record。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmEmployeeLeaveRecordController.java`、`src/main/java/com/tianye/hrsystem/controller/HrmEmployeeOverTimeRecordController.java`
- Mapper:`src/main/resources/mapper/HrmEmployeeLeaveRecordMapper.xml`、`HrmEmployeeOverTimeRecordMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmEmployeeLeaveRecord.java`、`HrmEmployeeOverTimeRecord.java`
- Vue:`src/views/hrm/attendance/leave/`

## 四、考勤管理

### 4.1 排班管理(矩阵/单日修改/上传导入/标准与自定义班次)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【排班管理:员工月度排班矩阵、单日单员工修改、多产品多岗位分配、排班上传导入、标准/自定义班次、提交异步任务与断点续传】,相关数据库表:tbplanlist、hrm_workplan_custom_shift、hrm_attendance_plan、hrm_attendance_shift、hrm_attendance_group、hrm_attendance_group_relation_dept、hrm_attendance_group_relation_employee、hrm_attendance_date_shift、hrm_attendance_history_shift。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/WorkPlanListController.java`、`src/main/java/com/tianye/hrsystem/controller/AttendanceShiftController.java`、`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceGroupController.java`、`HrmAttendanceGroupRelationDeptController.java`、`HrmAttendanceGroupRelationEmployeeController.java`、`HrmAttendancePointController.java`(同目录)
- Service:`src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`、`src/main/java/com/tianye/hrsystem/imple/ddTalk/AttendancePlanRecord.java`、`AttendanceDetailRecord.java`、`src/main/java/com/tianye/hrsystem/imple/AttendancePlanServiceImpl.java`、`AttendanceDetailServiceImpl.java` 及同目录相关实现
- Mapper:`src/main/resources/mapper/WorkPlanMapper.xml`、`HrmAttendancePlanMapper.xml`、`HrmEmpScheduleMapper.xml`、`HrmAttendanceShiftMapper.xml`、`HrmAttendanceGroupMapper.xml`、`HrmAttendanceDateShiftMapper.xml`、`HrmAttendanceHistoryShiftMapper.xml`、`HrmAttendanceGroupRelationDeptMapper.xml`、`HrmAttendanceGroupRelationEmployeeMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/tbplanlist.java`、`HrmAttendancePlan.java`、`HrmWorkPlanCustomShift.java`、`HrmAttendanceShift.java`、`HrmAttendanceGroup.java`
- Vue:`src/views/hrm/attendance/scheduling/Scheduling.vue`、`SchedulingDetail.vue`、`src/views/hrm/attendance/records/Records.vue`、`records/components/AddOrEdit.vue`（+`work-plan-utils.js`，2026-09-06 选人口径与穿梭框统一改动点）、`src/api/hrm/attendance/scheduling.js`、`workPlan.js`、`records.js`

### 4.2 生产产品配置(添加排班候选)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【生产产品配置:产品/岗位/员工三级配置树,供添加排班使用】,相关数据库表:hrm_workplan_product、hrm_workplan_product_position、hrm_workplan_position_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/workplan/controller/WorkPlanProductController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/workplan/service/impl/WorkPlanProductServiceImpl.java`
- Entity:`src/main/java/com/tianye/hrsystem/modules/workplan/entity/`
- Vue:`src/views/hrm/attendance/workplan-product/Index.vue`、`src/api/hrm/attendance/workplan-product.js`

### 4.3 排班申请审批
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【排班申请审批:排班调整申请的提交与审批流】,相关数据库表:hrm_workplan_application。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/WorkPlanApplicationController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/workplanapplication/service/impl/WorkPlanApplicationServiceImpl.java`
- Entity:`src/main/java/com/tianye/hrsystem/modules/workplanapplication/entity/`
- Vue:`src/views/hrm/attendance/workPlanApplication/Index.vue`、`src/api/hrm/attendance/workPlanApplication.js`

### 4.4 钉钉考勤/审批数据同步(员工管理-同步考勤、审批数据-获取审批)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【钉钉数据同步:同步考勤(7 步全量/增量+断点续传)、获取审批数据、按公司异步互斥、进度上报、失败自动重试、钉钉限流处理】,相关数据库表:tbattendanceuser、tbattendancedetail、hrm_attendance_clock、hrm_attendance_plan、hrm_attendance_group、tbattendanceapprove、hrm_attendance_approval_fetch_mark、hrm_attendance_report_data、hrm_attendance_report_field、postresultlog、ddtaskresult。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceDataController.java`、`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmAttendanceDataServiceImpl.java`、`src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImpl.java`、`src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalSyncServiceImpl.java`、`src/main/java/com/tianye/hrsystem/imple/ddTalk/`(AttendanceUserManager、AttendancePlanRecord、AttendanceDetailRecord、AttendanceLeaveRecord、AttendanceGroupManager、HrmAttendanceReportManager、DDAccessToken)、`src/main/java/com/tianye/hrsystem/task/AttendanceSyncTaskLauncher.java`、`src/main/java/com/tianye/hrsystem/task/DingTalkLogRetentionCleanupTask.java`、`src/main/java/com/tianye/hrsystem/autoTask/`(各定时刷新任务)
- Mapper:`src/main/resources/mapper/HrmAttendanceApprovalMapper.xml`、`HrmAttendanceReportDataMapper.xml`、`HrmAttendanceReportFieldMapper.xml` 等;`src/main/java/com/tianye/hrsystem/repository/`(tbattendanceuserRepository、tbattendancedetailRepository、hrmAttendanceClockRepository、postresultlogRepository、ddtaskresultRepository)
- Entity:`src/main/java/com/tianye/hrsystem/model/tbattendanceuser.java`、`tbattendancedetail.java`、`tbattendanceapprove.java`、`Postresultlog.java`、`src/main/java/com/tianye/hrsystem/model/ddTalk/Ddtaskresult.java`
- Vue:`src/views/hrm/employee/Index.vue`(同步考勤入口+进度)、`src/views/hrm/attendance/approval/Index.vue`(获取审批数据+进度)、`src/api/hrm/employee/employee.js`、`src/api/hrm/attendance/approval.js`

### 4.5 审批数据(列表/手工添加/统计状态)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【审批数据列表:月份/类型筛选、行内手工添加审批、统计状态(添加至统计/取消至统计)】,相关数据库表:tbattendanceapprove、hrm_attendance_approval_fetch_mark。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceApprovalController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalServiceImpl.java`、`HrmAttendanceApprovalSyncServiceImpl.java`、`HrmAttendanceApprovalProcessInstanceParser.java`
- Mapper:`src/main/resources/mapper/HrmAttendanceApprovalMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/tbattendanceapprove.java`
- Vue:`src/views/hrm/attendance/approval/Index.vue`、`src/api/hrm/attendance/approval.js`

### 4.6 打卡记录 / 员工月度汇总
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【打卡记录与员工月度汇总:打卡概况、明细查询、月度汇总导出】,相关数据库表:hrm_attendance_clock、tbattendancedetail、hrm_attendance_emp_month_record、hrm_attendance_report_data。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceClockController.java`、`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceEmpMonthRecordController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmAttendanceClockServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmAttendanceClockMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmAttendanceClock.java`、`tbattendancedetail.java`、`HrmAttendanceEmpMonthRecord.java`
- Vue:`src/views/hrm/attendance/clock/Clock.vue`、`src/views/hrm/attendance/summary/Summary.vue`、`src/api/hrm/attendance/clock.js`、`summary.js`

### 4.7 加班/夜班统计
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【加班/夜班统计:开始统计、单人统计、应出勤/实际出勤口径、矩阵查询、手工保存出勤小时、导出】,相关数据库表:hrm_overtime_night_statistics、hrm_overtime_night_statistics_detail、tbplanlist(排班唯一事实源,经hrm_workplan_custom_shift解析自定义班次)、hrm_attendance_clock、tbattendanceapprove、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmOvertimeNightStatisticsController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmOvertimeNightStatisticsServiceImpl.java`、`OvertimeNightClockResolver.java`、`OvertimeNightStatisticsExportSupport.java`(同目录)
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmOvertimeNightStatistics(.Detail).java`、`hrm_workplan_custom_shift`实体
- Vue:`src/views/hrm/attendance/overtime-night/Index.vue` 及同目录组件、`src/api/hrm/attendance/overtimeNight.js`

### 4.8 单双休设置
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【单双休设置:月历维护、法定节假日、生产体系月休天数】,相关数据库表:hrm_workweek_setting、hrm_workweek_day_setting、hrm_attendance_legal_holidays。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/workweek/controller/HrmWorkweekSettingController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/workweek/service/`
- Entity:`src/main/java/com/tianye/hrsystem/modules/workweek/entity/`
- Vue:`src/views/hrm/attendance/workweek/Index.vue`、`src/api/hrm/attendance/workweek.js`

### 4.9 考勤汇总(含行政体系考勤导出)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【考勤汇总:月度汇总列表/单元格编辑/导入/行政体系考勤导出】,相关数据库表:hrm_produce_attendance、hrm_employee、hrm_overtime_night_statistics_detail、tbattendanceapprove、hrm_employee_quit_info(行政导出去离职判定用离职日期)、hrm_salary_config(行政导出去离职判定用发薪日 payDay)。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmProduceAttendanceController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmProduceAttendanceServiceImpl.java`、`AdministrativeAttendanceExportSupport.java`(导出模板+单元格批注)；在职判定读 `modules/salary/service/HrmSalaryConfigService.java`(payDay)、离职日期读 `repository/hrmEmployeeQuitInfoRepository.java`
- Mapper:`src/main/java/com/tianye/hrsystem/mapper/HrmProduceAttendanceMapper.java`、`src/main/resources/mapper/HrmProduceAttendanceMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmProduceAttendance.java`、`src/main/java/com/tianye/hrsystem/entity/vo/AdministrativeAttendanceReportDetailVO.java`(导出批注用按天明细)、`model/HrmEmployeeQuitInfo.java`(离职日期)、`modules/salary/entity/HrmSalaryConfig.java`(发薪日)
- Vue:`src/views/hrm/attendance/upload/Upload.vue`、`src/api/hrm/attendance/upload.js`

### 4.10 考勤规则设置(管理端)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【考勤规则设置:行政/生产应出勤天数配置、考勤规则维护】,相关数据库表:hrm_attendance_rule、hrm_attendance_info、hrm_attendance_legal_holidays。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmAttendanceRuleController.java`、`src/main/java/com/tianye/hrsystem/modules/attendanceinfo/controller/HrmAttendanceInfoController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmAttendanceRuleServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmAttendanceRuleMapper.xml`、`HrmAttendanceInfoMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/HrmAttendanceRule.java`、`modules/attendanceinfo/entity/HrmAttendanceInfo.java`
- Vue:`src/views/manage/attendance/Index.vue`

## 五、社保管理

### 5.1 社保月报(生成/详情/一键设置保险金额)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【社保管理月报:生成次月社保报表、员工参保明细、一键设置/取消保险金额、进度查询】,相关数据库表:hrm_insurance_month_record、hrm_insurance_month_emp_record、hrm_insurance_month_emp_project_record、hrm_insurance_scheme、hrm_salary_basic。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/insurance/controller/HrmInsuranceMonthRecordController.java`、`HrmInsuranceMonthEmpRecordController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/insurance/service/HrmInsuranceMonthRecordService.java`、`HrmInsuranceMonthEmpRecordService.java`、`HrmInsuranceMonthEmpProjectRecordService.java`
- Mapper:`src/main/resources/mapper/HrmInsuranceMonthRecordMapper.xml`、`HrmInsuranceMonthEmpRecordMapper.xml`、`HrmInsuranceMonthEmpProjectRecordMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/insurance/entity/`(HrmInsuranceMonthRecord、HrmInsuranceMonthEmpRecord、HrmInsuranceMonthEmpProjectRecord)
- Vue:`src/views/hrm/insurance-scheme/InsuranceScheme.vue`、`InsuranceDetail.vue`、`src/api/hrm/insurance-scheme/detail.js`、`insurance-scheme.js`

### 5.2 社保方案管理(管理端)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【社保方案管理:养老/医疗/失业/工伤/生育/医疗长期护理等项目行配置、启用禁用、合计口径】,相关数据库表:hrm_insurance_scheme、hrm_insurance_project。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/insurance/controller/HrmInsuranceSchemeController.java`、`HrmInsuranceProjectController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/insurance/service/HrmInsuranceSchemeService.java`、`HrmInsuranceProjectServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmInsuranceSchemeMapper.xml`、`HrmInsuranceSechemeMapper.xml`(历史拼写)
- Entity:`src/main/java/com/tianye/hrsystem/modules/insurance/entity/HrmInsuranceScheme.java`、`HrmInsuranceProject.java`
- Vue:`src/views/manage/insurance-scheme/InsuranceScheme.vue`

## 六、薪资管理

### 6.1 薪资月报(开始核算/范围选择/导出/新建次月/恢复)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【薪资管理:开始核算(按人员/部门)、薪资列表、导出工资表(含批注)、新建次月薪资、薪资月恢复、核算进度】,相关数据库表:hrm_salary_month_record、hrm_salary_month_emp_record、hrm_salary_month_option_value、hrm_salary_option、hrm_salary_export、hrm_employee、hrm_produce_attendance、hrm_overtime_night_statistics_detail。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryMonthRecordController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryMonthRecordServiceNew.java`、`SalaryComputeServiceNew.java`、`TaxCalculator.java`、`src/main/java/com/tianye/hrsystem/modules/salary/support/SalaryExportCommentWriteHandler.java`(导出批注)、`src/main/java/com/tianye/hrsystem/imple/HrmSalaryMonthRecordServiceImpl.java`、`HrmSalaryMonthOptionValueServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmSalaryMonthRecordMapper.xml`、`HrmSalaryMonthEmpRecordMapper.xml`、`HrmSalaryMonthOptionValueMapper.xml`、`HrmSalaryOptionMapper.xml`、`HrmSalaryExportMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/salary/entity/`(HrmSalaryMonthRecord、HrmSalaryMonthEmpRecord 等)
- Vue:`src/views/hrm/salary/salary/SalaryManage.vue`、`src/views/hrm/salary/Index.vue`、`src/api/hrm/salary/salary.js`

### 6.2 薪资档案
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【薪资档案:定薪、上传调薪/定薪(Excel导入,沿用watch模板格式)、下载模板、下载数据、档案明细(基本/岗位/职务工资、试用期与正式两套)、在职离职筛选】,相关数据库表:hrm_salary_archives、hrm_salary_archives_option、hrm_salary_change_record、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryArchivesController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/salary/service/HrmSalaryArchivesService.java`(importSalaryFixing/downloadSalaryFixingTemplate/downloadSalaryFixingData)
- Mapper:`src/main/resources/mapper/HrmSalaryArchivesMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/salary/entity/HrmSalaryArchives(.Option).java`
- 模板:`src/main/resources/export/调薪定薪导入模板.xlsx`
- Vue:`src/views/hrm/salary/archives/Archives.vue`、`src/api/hrm/salary/salary.js`

### 6.3 历史工资
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【历史工资:历史月份查询与明细】,相关数据库表:hrm_salary_month_record、hrm_salary_month_emp_record、hrm_salary_month_option_value。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryHistoryRecordController.java`
- Mapper:`src/main/resources/mapper/HrmSalaryMonthRecordMapper.xml`
- Vue:`src/views/hrm/salary/history/History.vue`、`HistoryDetail.vue`、`src/api/hrm/salary/history.js`

### 6.4 工资条
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【工资条:模板配置、发放记录、发送】,相关数据库表:hrm_salary_slip_record、hrm_salary_slip_template、hrm_salary_slip_template_option。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalarySlipRecordController.java`、`HrmSalarySlipTemplateController.java`、`HrmSalarySlipTemplateOptionController.java`
- Mapper:`src/main/resources/mapper/HrmSalarySlipMapper.xml`、`HrmSalarySlipRecordMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/salary/entity/`(HrmSalarySlip 相关)

### 6.5 个税累计 / 附加累计 / 年度附加扣除
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【个税与专项附加:个税累计导入、附加累计导入、年度附加扣除导入、模板下载】,相关数据库表:hrm_personal_income_tax、hrm_additional、hrm_employee_additional。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/deduction/controller/HrmPersonalIncomeTaxController.java`、`src/main/java/com/tianye/hrsystem/modules/additional/controller/HrmAdditionalController.java`、`HrmEmployeeAdditionalController.java`
- Mapper:`src/main/resources/mapper/HrmPersonalIncomeTaxMapper.xml`、`HrmAdditionalMapper.xml`、`HrmEmployeeAdditionalMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/deduction/entity/`、`modules/additional/entity/`
- Vue:`src/views/hrm/salary/tax/Tax.vue`、`addition/Addition.vue`、`additionDeduction/Index.vue`、`src/api/hrm/salary/tax.js`、`addition.js`、`additionDeduction.js`

### 6.6 基本工资金额设置(管理端)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【基本工资金额设置:普通/领导全勤金额、生产体系月休天数、大额医疗、长期护理、按人员/部门设置、与薪资档案同步】,相关数据库表:hrm_salary_basic、hrm_salary_config、hrm_salary_change_template、hrm_salary_archives_option、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/salary/controller/HrmSalaryBasicController.java`、`HrmSalaryConfigController.java`、`HrmSalaryChangeTemplateController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/salary/service/impl/HrmSalaryConfigServiceImpl.java`
- Mapper:`src/main/resources/mapper/HrmSalaryBasicMapper.xml`、`HrmSalaryConfigMapper.xml`、`HrmSalaryChangeTemplateMapper.xml`、`HrmSalaryChangeRecordMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/salary/entity/`(HrmSalaryBasic、HrmSalaryConfig、HrmSalaryChangeTemplate)
- Vue:`src/views/manage/salary/Index.vue`

### 6.7 奖金中心
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【奖金中心:上传奖金(累加至工资计税)/上传奖金(只计税)导入】,相关数据库表:hrm_bonus、hrm_bonus_tax_only、hrm_employee。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/bonus/controller/HrmBonusController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/bonus/service/HrmBonusService.java`
- Mapper:`src/main/resources/mapper/HrmBonusMapper.xml`、`HrmBonusTaxOnlyMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/bonus/entity/HrmBonus(.TaxOnly).java`
- Vue:`src/views/hrm/bonus/Index.vue`、`src/api/hrm/bonus/index.js`

### 6.8 假期管理(管理端)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【假期管理:剩余假期维护、假期扣款设置】,相关数据库表:hrm_remaining_vacation、hrm_holiday_deduction。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/holiday/controller/HrmRemainingVacationController.java`、`HrmHolidayDeductionController.java`
- Mapper:`src/main/resources/mapper/HrmRemainingVacationMapper.xml`、`HrmHolidayDeductionMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/modules/holiday/entity/`
- Vue:`src/views/manage/vacation/Index.vue`

## 七、基础与系统功能

### 7.1 数据配置
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【数据配置:自定义类型/值字典维护】,相关数据库表:tbdictdata、hrm_config、tbcompanylist。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/DictDataController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/tbDictDataServiceImpl.java`、`HrmConfigServiceImpl.java`
- Mapper:`src/main/resources/mapper/DIctDataMapper.xml`、`HrmDictDataMapper.xml`、`HrmConfigMapper.xml`
- Entity:`src/main/java/com/tianye/hrsystem/model/tbDictData.java`
- Vue:`src/views/hrm/dataConfig/Index.vue`、`src/api/hrm/dataConfig/index.js`

### 7.2 零时工
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【零时工:零时工数据导出】,相关数据库表:tblregister、tbweight、tbworkshop(零时工业务库)。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/tempworker/controller/TempWorkerExportController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/tempworker/service/TempWorkerExportService.java`
- Mapper:`src/main/resources/mapper/TempWorkerExportMapper.xml`
- Vue:`src/api/`(按需新增)

### 7.3 附件管理
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【附件管理:附件上传(FTP)、下载、临时文件清理】,相关数据库表:tbattachment。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/AttachmentController.java`、`src/main/java/com/tianye/hrsystem/controller/AdminFileController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/AttachmentServiceImpl.java`、`AdminFileServiceImpl.java`、`FtpFileServiceImpl.java`、`LocalFileServiceImpl.java`
- 公共组件:`src/main/java/com/tianye/hrsystem/common/FTPUtil.java`、`WebFileUtils.java`、`UploadUtils.java`
- Entity:`src/main/java/com/tianye/hrsystem/model/tbattachment.java`

### 7.4 钉钉 API 调用量监控
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【钉钉API调用量监控:月度用量统计、阈值提醒、功能分布】,相关数据库表:postresultlog、ddtaskresult。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/HrmDingTalkApiUsageController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/HrmDingTalkApiUsageServiceImpl.java`、`src/main/java/com/tianye/hrsystem/common/DDTalkResposeLogger.java`
- Entity:`src/main/java/com/tianye/hrsystem/model/Postresultlog.java`、`src/main/java/com/tianye/hrsystem/model/ddTalk/Ddtaskresult.java`
- 清理任务:`src/main/java/com/tianye/hrsystem/task/DingTalkLogRetentionCleanupTask.java`
- Vue:`src/components/layout/TopHeader.vue`(顶栏提醒弹窗)、`src/api/hrm/dingtalkApiUsage.js`、`src/utils/dingtalk-api-usage.js`

### 7.5 数据库备份
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【数据库备份:手动/自动备份、还原、清理、备份记录】,相关数据库表:tb_backup_record。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/modules/backup/controller/DatabaseBackupController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/backup/service/DatabaseBackupService.java`、`src/main/java/com/tianye/hrsystem/autoTask/DatabaseBackupTask.java`
- Vue:`src/views/hrm/system/DatabaseBackup.vue`

### 7.6 动态定时调度
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【动态定时调度:按 code 启停 cron 定时回调】,相关数据库表:hrm_scheduled。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/ScheduledController.java`
- Mapper:`src/main/resources/mapper/HrmScheduledMapper.xml`
- Entity/VO:`src/main/java/com/tianye/hrsystem/entity/vo/HrmScheduledVo.java`

### 7.7 钉钉集成基础(Token/限流/消息日志)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【钉钉集成基础: AccessToken 管理、调用日志、限流器、响应日志表清理】,相关数据库表:postresultlog。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Service:`src/main/java/com/tianye/hrsystem/imple/ddTalk/DDAccessToken.java`、`src/main/java/com/tianye/hrsystem/service/ddTalk/`(IAccessToken 等接口)、`src/main/java/com/tianye/hrsystem/autoTask/common/DingTalkRateLimiter.java`、`AbstractDingTalkTask.java`
- Entity:`src/main/java/com/tianye/hrsystem/model/Postresultlog.java`、`repository/postresultlogRepository.java`

### 7.8 小程序(移动端登录/排班)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【小程序端:微信 OpenID 登录、首次员工身份绑定、多公司切换、员工排班查询等移动端能力】,相关数据库表:hrm_employee(openid)、tbCompanyList、tbplanlist(含 Redis 临时凭证键)。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/MiniAppController.java`
- Service:`src/main/java/com/tianye/hrsystem/modules/miniapp/service/impl/MiniAppServiceImpl.java`、`MiniAppScheduleServiceImpl.java`、`src/main/java/com/tianye/hrsystem/modules/miniapp/support/MiniAppWxClient.java`
- Mapper/Entity:`src/main/java/com/tianye/hrsystem/modules/miniapp/mapper/MiniAppEmployeeMapper.java`、`MiniAppSystemMapper.java`、`src/main/java/com/tianye/hrsystem/model/HrmEmployee.java`
- Vue:小程序端 `miniapp/hr_miniapp/src/pages/login/login.vue`、`src/api/modules/miniapp.js`

### 7.9 通知中心(系统消息通知)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【通知中心:系统级消息通知(考勤同步/审批获取/社保计算等任务状态),支持未读计数、标记已读、删除、跳转链接】,相关数据库表:admin_message。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`src/main/java/com/tianye/hrsystem/controller/AdminMessageController.java`
- Service:`src/main/java/com/tianye/hrsystem/imple/AdminMessageServiceImpl.java`、`src/main/java/com/tianye/hrsystem/service/IAdminMessageService.java`
- Mapper:`src/main/resources/mapper/AdminMessageMapper.xml`、`src/main/java/com/tianye/hrsystem/mapper/AdminMessageMapper.java`
- Entity:`src/main/java/com/tianye/hrsystem/entity/po/AdminMessage.java`、`src/main/java/com/tianye/hrsystem/entity/bo/AdminMessageBO.java`、`src/main/java/com/tianye/hrsystem/enums/AdminMessageEnum.java`
- Vue:`src/components/NotificationCenter.vue`、`src/views/notification/NotificationList.vue`、`src/components/layout/TopHeader.vue`、`src/api/hrm/notification.js`、`src/router/config.js`

### 7.10 算法知识库(顶栏弹窗,纯前端)
**Prompt:**
这是 HR 系统,我要修改【顶栏算法知识库:展示后端全部业务算法/计算公式(公式、豁免规则、代码位置),支持关键词搜索匹配,按模块分组】,纯前端无后端接口。只输出需要修改的 Vue/JS 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- 数据:`hr_web/src/constants/knowledgeBase.js`(28 条算法条目,后端算法改动须同步)
- Vue:`hr_web/src/components/KnowledgeBaseDialog.vue`(搜索+模块分组弹窗)、`hr_web/src/components/layout/TopHeader.vue`(Collection 图标按钮)
- 算法来源(后端,只读参考):`hainan/src/main/java/com/tianye/hrsystem/modules/salary/service/SalaryComputeServiceNew.java`、`TaxCalculator.java`、`SalaryMonthRecordServiceNew.java`、`HrmOvertimeNightStatisticsServiceImpl.java` 等

### 7.11 到龄退休提醒
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【到龄退休提醒:按出生日期推算退休日期(男63/女58),员工本月到龄时发站内信,支持定时/页面刷新钩子/手动触发三种入口】,相关数据库表:admin_message(现有表,无 DDL)。只输出需要修改的 Controller、Task、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Task:`hainan/src/main/java/com/tianye/hrsystem/autoTask/RetirementReminderTask.java`(推算+发信+按月去重+三入口)
- Controller:`hainan/src/main/java/com/tianye/hrsystem/controller/AdminMessageController.java`(unreadCount 前置钩子,主路径)、`hainan/src/main/java/com/tianye/hrsystem/controller/HrmEmployeeController.java`(/retirementRemind 手动触发)
- 枚举:`hainan/src/main/java/com/tianye/hrsystem/enums/AdminMessageEnum.java`(type=207 HRM_EMPLOYEE_RETIREMENT_REMIND)
- Vue:`hr_web/src/components/NotificationCenter.vue`(铃铛展示,未改动)

### 7.12 排班小程序权限(员工级授权+审批可见范围)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【排班小程序权限:PC 配置页按员工授予"可添加排班"与审批可见范围三档(直属下属/全部员工/自定义指定),租户级菜单开关控制 /mp/mySchedule 等端点】,相关数据库表:mp_schedule_permission、mp_schedule_visible_employee、tbmenu(1021/5000/5020)、tb_api_permission、hrm_employee(parent_id 直属上级)。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- Controller:`hainan/src/main/java/com/tianye/hrsystem/modules/miniapp/controller/MiniAppPermissionController.java`(/mpPermission/employeeList|save)、`controller/MiniAppController.java`(isSupervisor 返回 {isSupervisor,canSchedule}/save/employees 权限校验)、`config/CompanyInterceptor.java`(租户级菜单开关分支)
- Service:`hainan/src/main/java/com/tianye/hrsystem/modules/miniapp/service/IMiniAppPermissionService.java` 及 impl、`WorkPlanApplicationServiceImpl.java`(listToApprove 三档可见范围；done=我批的∪可见已处理并集)
- SQL:`hainan/docs/sql/2026-09-06_miniapp_schedule_permission.sql`(存量租户补数)、`schema-baseline.sql`/`seed-data.sql`/`api-permission-seed.sql`
- Vue:`hr_web/src/views/hrm/system/MiniappPermission.vue`(员工列表+穿梭框选员工:按人员/按部门)、`hr_web/src/api/hrm/system/permission.js`
- 小程序:`miniapp/hr_miniapp/src/pages/index/index.vue`(添加排班入口按 canSchedule 显隐)、`miniapp/hr_miniapp/src/pages/addschedule/addschedule.vue`(onLoad canSchedule 门禁)、`miniapp/hr_miniapp/src/api/modules/miniapp.js`(isSupervisor)

### 7.13 本地考勤判定引擎(弃用钉钉排班推送)
**Prompt:**
这是 SpringBoot 多租户 SaaS 系统,我要修改【本地考勤判定:弃用提交排班到钉钉(开关 hrm.dingtalk.schedule-push.enabled 默认 false),以 tbplanlist 为应出勤基准本地判定迟到/早退/缺卡/旷工;夜班(customShiftPeriod=night)即跨天班;宽松取卡口径(最早上班卡/最晚下班卡),迟到缺卡互斥,旷工=全天无卡;补卡取自审批数据(tagName 含"补卡"),每人每月前 N 张生效(N=考勤规则 max_monthly_card_repair);阈值/窗口接入考勤规则设置;员工保存时按姓名+手机号映射 dingtalk_user_id,查无此人拒保存(排班域不再读写 tbattendanceuser,考勤域保留)】,相关数据库表:hrm_attendance_judge_result、hrm_attendance_rule(judge_window_before/after_minutes、max_monthly_card_repair)、tbplanlist、tbattendancedetail、tbattendanceapprove、hrm_employee(dingtalk_user_id)、tbattendanceuser(仅考勤域)。只输出需要修改的 Controller、Service、Mapper、Entity、Vue 文件完整路径列表,不要生成修改代码,不要执行修改。

**参考文件清单:**
- 判定引擎:`hainan/src/main/java/com/tianye/hrsystem/imple/HrmAttendanceJudgeServiceImpl.java`、`service/IHrmAttendanceJudgeService.java`
- 推送开关/排班身份:`hainan/src/main/java/com/tianye/hrsystem/imple/WorkPlanServiceImpl.java`(submitPlans 开关分支、resolveAttendanceUserByEmployeeId 内存身份、loadUsersFromLocalSnapshot/resolveEmployeeContinuousShiftByUserId 双键)
- 映射前置:`hainan/src/main/java/com/tianye/hrsystem/imple/HrmAttendanceApprovalSyncServiceImpl.java`(ensureDingTalkUserId)、`imple/employee/HrmEmployeeServiceImpl.java`(add/updateInformation/updateCommunication 钩子)、`autoTask/DingTalkUserMappingRetryTask.java`(每日重试+remapEmployees)、`common/EmployeeNotInDingTalkException.java`
- 降表依赖:`modules/miniapp/service/impl/MiniAppScheduleServiceImpl.java`、`modules/workplanapplication/service/impl/WorkPlanApplicationServiceImpl.java`、`modules/workplan/service/impl/WorkPlanProductServiceImpl.java`、`controller/WorkPlanListController.java`
- 接口:`controller/HrmAttendanceDataController.java`(/attendanceData/judgeRecompute)
- SQL:`hainan/docs/sql/2026-09-06_attendance_judge.sql`(租户库)

---

## 附:公共横切组件(多模块共用,改动需评估影响面)

| 组件 | 路径 | 说明 |
|---|---|---|
| 多租户拦截/上下文 | `hainan/src/main/java/com/tianye/hrsystem/config/CompanyInterceptor.java`、`CompanyContext.java` | 令牌校验、租户上下文、API 菜单权限 |
| API-菜单权限映射 | `hainan/src/main/java/com/tianye/hrsystem/modules/menu/service/ApiPermissionPathSupport.java` | 新增接口必须登记映射,否则绕过菜单权限 |
| 统一返回/异常 | `hainan/src/main/java/com/tianye/hrsystem/model/successResult.java`、`modules/**/vo/Result.java`、`config/GlobalExceptionHandler.java` | |
| 前端请求层 | `hr_web/src/api/requset.js`、`base-url.js` | token 头注入、会话失效处理 |
| 前端路由/权限 | `hr_web/src/router/config.js`、`router/router.js`、`utils/permission.js` | 新页面需登记路由;`DEFAULT_ALLOWED_PATHS` 为全角色白名单 |
| 前端布局 | `hr_web/src/components/layout/TopHeader.vue`、`SideMenu.vue`、`MenuTab.vue`、`Layout.vue` | 顶栏/侧栏/页签 |
| 全局弹窗 | `hr_web/src/components/RestPassword.vue`(改密)、`Table.vue`、`Dialog.vue` | |
| 导入导出工具 | `hainan/src/main/java/com/tianye/hrsystem/common/ExcelImportUtil.java`(EasyExcel 流式)、`WebFileUtils.java` | ⚠️ Excel 批量导入功能按业务要求不做改动 |
