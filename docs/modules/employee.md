# 员工管理（部门 / 花名册 / 员工合同 / 招聘渠道 / 员工薪资字段）

菜单前缀：`/hrm/dept`、`/hrm/employee*`、`/hrmRecruitChannel`、`/hrmEmployeeContract`、`/hrmEmployeePost`。

## 需求要点
- 唯一性：未删除员工范围内工号/手机号/身份证号唯一（身份证去空格大写、手机号去空格和 `-` 后比较），新增保留前端工号不覆盖为时间戳；花名册导入先整文件校验再落库。
- 花名册导入按"表头驱动"：读取激活的可见工作表（第 1 行分组、第 2 行字段名、第 3 行起数据），`姓名+手机号` 匹配更新，`姓名+身份证号` 兜底命中避免手机号变更误判新员工；公式列（如 DATEDIF）失败回退缓存值不中断导入；未知列自动建自定义字段，重复表头用"分组-字段名"。
- 员工合同：无固定期限劳动合同（contractType=2）结束日期可空，保存/导入时清空 `endTime/term`；其他类型起止必填、结束不得早于开始；期限 `term` 由服务端按日期自动计算（年）并覆盖前端值；导入按"姓名+电话"匹配，重复合同（员工+类型+开始日期+结束日期）自动跳过并返回跳过数量；支持一键清理重复合同（保留最新的）。
- 员工薪资字段（薪资等级/固定绩效/职务补助/其他补助）全部走动态字段 `hrm_employee_field`/`hrm_employee_data`，不加物理列；金额字段两位小数、类型 DECIMAL 精度 2，后端幂等补齐/纠正字段定义；新建、再次入职、详情编辑、办理转正/调岗/晋升降级均可维护。
- 下载部门明细：全公司未删除在职员工多 sheet Excel（人员总表 + 部门明细），薪资待遇 = 10101+10102+10103+固定绩效+职务补助+其他补助+全勤（`full_attendance=1` 且非试用期、金额≠100）；成本区含固定/绩效薪资、社保、公积金（伙食已删除）。
- 行政经理（roleName 严格等于）跨公司导出：遍历 0001/0003/0004/0005（成都 0002 不参与）切换 `CompanyContext` 分别生成明细 + `集团总表.xlsx`，Zip4j AES-256 加密，密码经 `X-Archive-Password` 响应头返回（CORS 需暴露该头）。
- 组织编码：`/hrmDept/generateCode` 返回最小未使用正整数（编辑排除自身），保存时后端强制重算覆盖。

## 设计与契约
- 员工表关键字段：`affiliation_system`（1 行政/2 生产）、`rest_type`（1 单双休/2 固定月休4）、`is_continuous_shift`、`full_attendance`、`is_disabled`、`is_retired_soldier`、`is_party_member`、`personnel_category`（1财务/2管理/3技术/4生产/5行政）、`ordinary/leader_full_attendance_amount`、`company_age_start_time`、`dingtalk_user_id`。
- 列表 `queryPageList` 返回 Map：固定列必须显式驼峰别名；身份证兜底派生生日/年龄；动态字段空 key 跳过；司龄按 `company_age_start_time`（缺省回退 entry_time）实时计算。
- 司龄口径：默认从入职日期起算（试用期计入）；`company_age_start_time` 非空则忽略入职日期按该字段算，**清空该字段即回退入职日期**——详情编辑保存时显式置 NULL（updateById 忽略 null 字段的坑已处理），转正日期不参与司龄计算。
- 主要端点：`/hrmEmployee/queryPageList|import|exportBasicInfoTemplate|exportDepartmentDetail|downloadEmployeeRosterTemplate|updateInsuranceScheme|departmentDetailGroupSummary|listForBatchSetting|batchSetting/save|syncDingTalkRoster`、`/hrmEmployeeContract/import|downloadContractTemplate|addContract|setContract|queryDuplicateContracts|deleteDuplicateContracts`、`/hrmDept/generateCode|queryTreeList|syncDingTalkDept|getExcludeNames|saveExcludeNames`。
- 批量设置：`POST /hrmEmployee/batchSetting/save`，fieldName 白名单 10 字段（fullAttendance/expandProduction/isDisabled/isContinuousShift/isRetiredSoldier/isPartyMember/personnelCategory/isRemark/affiliationSystem/restType），后端 switch 映射 setter 防注入；前端弹窗支持按员工（穿梭框，与同步考勤同数据源，默认带入列表勾选）/按部门（DeptSelect 多选，`listForBatchSetting` 预取人数与员工）两种范围。批量设置/批量参保/导入导出/同步钉钉员工收纳在筛选行"员工操作"弹出面板（批量操作/导入导出/钉钉同步三组）；其中 9 个字段（人员分类除外）已加入 `queryPageList` 返回列，筛选行"显示列"弹窗（带名称搜索框）可自定义显隐，偏好经 `/dashboard/userConfig/save`（boardKey=`employee_visible_batch_columns`，复用 `hrm_user_dashboard_config`）按登录用户持久化并本地 localStorage 双写。同步钉钉员工预检报告与"确认执行同步"合并为单弹窗，确认按钮恒显（有无变化由用户判断）。钉钉接口拉取失败重试 3 次后报错中止，不再静默吞错（曾因此导致权限缺失时预检假 0）。新建在职员工查无钉钉此人拒绝保存：`GlobalExceptionHandler` 对 `EmployeeNotInDingTalkException` 直返真实原因（HTTP 200 + success:false，不套"系统繁忙"），前端提交需校验 success。钉钉权限类错误（errcode 88/60011 等，`EmployeeNotInDingTalkException.isDingTalkPermissionError` 识别）在新建员工校验、员工同步、部门同步三处统一转为用户指引「钉钉应用未开通通讯录权限，请联系管理员开通」，不再静默放行。
- 同步钉钉员工（`HrmEmployeeDingTalkSyncService.syncRoster(dryRun)`）：`dryRun=true` 预检不落库。匹配顺序 dingtalk_user_id → 手机号 → 姓名唯一兜底；匹配不唯一/钉钉无手机号一律跳过进冲突清单。字段口径（2026-09-06 客户确认）：**姓名/手机号/部门/工号以系统为准绝不修改**，预检报告仅提示姓名/手机号/部门的差异（系统值 vs 钉钉值）供人工核对；**源头字段岗位/邮箱/入职日期/工作地点/userid 以钉钉为准覆盖（钉钉值为空不覆盖）**；系统独有字段一律不动。钉钉部门按名称匹配本地，匹配不到则该员工不关联部门并提示。凭据走 ddAccount（DDAccessToken）。
- 同步钉钉部门（`HrmDeptDingTalkSyncService`）：`topapi/v2/department/listsub` 递归全量，按名称 upsert，**归属关系以钉钉为准覆盖**（仅本地根节点自身不动；本地根多顶级时优先选 deptType=1 且创建最早）。**全量模式=完全覆盖**：本地不在钉钉的部门（根节点除外）删除；分公司模式：命中排除关键字的部门跳过同步且本地一并删除。两种模式删除部门时子树内员工自动转移到根部门并提示；同名部门按"未占用行优先认领"匹配，多次同步幂等不产生重复行。排除关键字 branch 分公司模式存 `hrm_dept_sync_config` 表（key=`dingtalk_exclude_names`）；DDL 见 `docs/sql/2026-09-06_dingtalk_sync_and_batch_setting.sql`（MySQL 8 兼容写法，不支持 ADD COLUMN IF NOT EXISTS）；新租户基线 `sql/tenant/schema-baseline.sql` 已含该表与 userid 索引。前端组织管理树默认收起（不再 default-expand-all）。
- 模板资源：`export/employee_module.xlsx`（运行时插列 薪资等级/固定绩效/职务补助/其他补助）、`export/hetong_module.xlsx`、`export/副本人资系统导出员工基础信息模版.xlsx`。
- 子女信息：`EmployeeChildrenInfoFieldFactory` 定义 `children_info`（fieldId=900001）+ 三个明细子字段，复用 `detail_table` 动态字段能力。
- 到龄退休提醒：口径男 63/女 58（延迟退休目标年龄，不区分工人/干部）。`RetirementReminderTask`（autoTask）对"本月到龄"的未删除在职员工（有性别+出生日期、entryStatus≠4）发站内信（type=207 `HRM_EMPLOYEE_RETIREMENT_REMIND`，label=8，link /hrm/employee），Redis key `retirement:remind:{公司}:{员工id}:{yyyyMM}` 去重（40 天过期）。三个触发入口：①`/adminMessage/unreadCount` 前置钩子 `checkCompanyQuietly`（每公司每天一次 Redis 闸门 `retirement:checked:{公司}:{日期}`，完成后恢复原租户上下文）——**主路径**；②`@Scheduled` 每天 9 点全租户（受全项目 `scheduling.enabled=false` 限制，各环境均关）；③手动 `POST /hrmEmployee/retirementRemind`。开关 `hrm.retirement-reminder.enabled`（默认开）。

## 近期变更
- 2026-09-07 分管领导↔直属上级联动：①组织管理保存（`/hrmDept/addDept|setDept` → `addOrUpdate`）时，若设置分管领导（`hrm_dept.leader_employee_id`），部门下全部未删除员工直属上级（`hrm_employee.parent_id`）同步改为该分管领导（为空不改动）；②新端点 `/hrmDept/queryDeptLeader/{deptId}`（返回 `DeptLeaderVO{leaderEmployeeId, leaderName}`）；③员工新建（`fillParentIdFromDeptLeader` 兜底）、花名册导入（新员工未填直属上级时兜底）、岗位信息编辑（`updatePostInformation`：部门变更且本次未提交 parent_id 时自动改为新部门分管领导）三处自动取分管领导；④前端选部门自动带出：新建员工 `AddOrEdit.vue`、部门添加员工 `DepAddEmployeeDialog.vue`、员工详情编辑 `DynamicForm.vue`（组件级统一挂接 dept_id 变化→取分管领导填 parent_id，详情回填不覆盖已有值）。⑤自指防护：部门保存同步员工、新建/导入兜底、编辑部门变更带出三处均排除"分管领导是本人"（自己不能是自己的上级）；⑥部门详情页 `DeptItem.vue` 分管领导绑定字段名修正 `leaderEmployeeName`→`leadEmployeeName`（此前详情页分管领导恒空白、编辑弹窗正常）。无表结构变更。
- 2026-09-06 watch 功能迁移三件套上线（经多轮口径演进，最终实现见上方契约）：①员工批量设置（10 字段白名单，实体补 is_disabled/is_retired_soldier/is_party_member/personnel_category 四列）+列表字段列与显示列自定义；②同步钉钉员工（userid 绑定+姓名/手机号/部门/工号不改仅预检提示+源头字段覆盖+预检恒显确认按钮+拉取失败重试报错）；③组织管理同步钉钉数据（全量完全覆盖/分公司关键字删除+员工自动转根部门+同名幂等）；配套新建员工钉钉查无此人拒绝保存与权限错误用户指引。DDL：`docs/sql/2026-09-06_dingtalk_sync_and_batch_setting.sql`。
- 2026-09-06 到龄退休推算与站内信提醒上线：新增 `RetirementReminderTask`（男63/女58口径，本月到龄发站内信 type=207，Redis 按月去重）。触发主路径为 `/adminMessage/unreadCount` 前置钩子（进页面即检查，每公司每天一次）；另有 9 点定时（被全项目 `scheduling.enabled=false` 抑制）与手动端点 `POST /hrmEmployee/retirementRemind` 兜底。知识库"员工"页签同步新增退休口径条目。
- 2026-09-05~09-03：员工调岗筛选月份参数（`queryEmployeeListForTransfer`+planQuitTime）、个人基本信息 Invalid bound statement 修复、合同导入去重与清理重复合同、组织管理改部门名数据消失修复（详见 archive/changelog.md）。

## 历史摘要
- 2026-09-06：watch 功能迁移三件套（批量设置 10 字段、同步钉钉员工/部门、hrm_dept_sync_config，DDL `2026-09-06_dingtalk_sync_and_batch_setting.sql`）。
- 2026-08-19~31：数据看板集团总表 404、司龄口径修复、田野农谷 9 项修复、部门明细员工级全勤开关、员工其他补助；部门明细导出上线、批量设置参保方案、员工薪资动态字段。
- 2026-06-10~16：员工编辑部门/学历保存修复、司龄/生日口径与列表驼峰字段、高级查询增强、合同新增不覆盖与期限自动计算、组织编码自动生成、花名册字段比对、花名册导入重构、子女信息、离职状态列、基础信息模板导出、合同导入、固定模板下载、按钮布局优化。
- 2026-07-17：唯一性校验统一 + 删除"是否有全勤/是否加入钉钉"表单项。更早细节见 `../archive/changelog.md`。
