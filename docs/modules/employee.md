# 员工管理（部门 / 花名册 / 员工合同 / 招聘渠道 / 员工薪资字段）

菜单前缀：`/hrm/dept`、`/hrm/employee*`、`/hrmRecruitChannel`、`/hrmEmployeeContract`、`/hrmEmployeePost`。

## 需求要点
- 唯一性：未删除员工范围内工号/手机号/身份证号唯一（身份证去空格大写、手机号去空格和 `-` 后比较），新增保留前端工号不覆盖为时间戳；花名册导入先整文件校验再落库。
- 花名册导入按"表头驱动"：读取激活的可见工作表（第 1 行分组、第 2 行字段名、第 3 行起数据），`姓名+手机号` 匹配更新，`姓名+身份证号` 兜底命中避免手机号变更误判新员工；公式列（如 DATEDIF）失败回退缓存值不中断导入；未知列自动建自定义字段，重复表头用"分组-字段名"。
- 员工合同：无固定期限劳动合同（contractType=2）结束日期可空，保存/导入时清空 `endTime/term`；其他类型起止必填、结束不得早于开始；期限 `term` 由服务端按日期自动计算（年）并覆盖前端值；导入按"姓名+电话"匹配，一律新增不覆盖。
- 员工薪资字段（薪资等级/固定绩效/职务补助/其他补助）全部走动态字段 `hrm_employee_field`/`hrm_employee_data`，不加物理列；金额字段两位小数、类型 DECIMAL 精度 2，后端幂等补齐/纠正字段定义；新建、再次入职、详情编辑、办理转正/调岗/晋升降级均可维护。
- 下载部门明细：全公司未删除在职员工多 sheet Excel（人员总表 + 部门明细），薪资待遇 = 10101+10102+10103+固定绩效+职务补助+其他补助+全勤（`full_attendance=1` 且非试用期、金额≠100）；成本区含固定/绩效薪资、社保、公积金（伙食已删除）。
- 行政经理（roleName 严格等于）跨公司导出：遍历 0001/0003/0004/0005（成都 0002 不参与）切换 `CompanyContext` 分别生成明细 + `集团总表.xlsx`，Zip4j AES-256 加密，密码经 `X-Archive-Password` 响应头返回（CORS 需暴露该头）。
- 组织编码：`/hrmDept/generateCode` 返回最小未使用正整数（编辑排除自身），保存时后端强制重算覆盖。

## 设计与契约
- 员工表关键字段：`affiliation_system`（1 行政/2 生产）、`rest_type`（1 单双休/2 固定月休4）、`is_continuous_shift`、`full_attendance`、`is_disabled`、`ordinary/leader_full_attendance_amount`、`company_age_start_time`、`dingtalk_user_id`。
- 列表 `queryPageList` 返回 Map：固定列必须显式驼峰别名；身份证兜底派生生日/年龄；动态字段空 key 跳过；司龄按 `company_age_start_time`（缺省回退 entry_time）实时计算。
- 司龄口径：默认从入职日期起算（试用期计入）；`company_age_start_time` 非空则忽略入职日期按该字段算，**清空该字段即回退入职日期**——详情编辑保存时显式置 NULL（updateById 忽略 null 字段的坑已处理），转正日期不参与司龄计算。
- 主要端点：`/hrmEmployee/queryPageList|import|exportBasicInfoTemplate|exportDepartmentDetail|downloadEmployeeRosterTemplate|updateInsuranceScheme|departmentDetailGroupSummary`、`/hrmEmployeeContract/import|downloadContractTemplate|addContract|setContract`、`/hrmDept/generateCode|queryTreeList`。
- 模板资源：`export/employee_module.xlsx`（运行时插列 薪资等级/固定绩效/职务补助/其他补助）、`export/hetong_module.xlsx`、`export/副本人资系统导出员工基础信息模版.xlsx`。
- 子女信息：`EmployeeChildrenInfoFieldFactory` 定义 `children_info`（fieldId=900001）+ 三个明细子字段，复用 `detail_table` 动态字段能力。

## 近期变更
- 2026-08-31 数据看板集团总表接口 404：根因是服务层（`HrmEmployeeServiceImpl#departmentDetailGroupSummary`，内部含行政经理与 `groupSummary:*` 看板权限校验）与前端均已实现，但 `HrmEmployeeController.java` 漏建端点；修复为补 `POST /hrmEmployee/departmentDetailGroupSummary` 返回 `Result.ok(...)`，权限=员工管理菜单+方法内校验。
- 2026-08-31 司龄口径确认与清空保存修复：确认司龄默认按入职日期起算、司龄开始日期非空则优先；修复详情清空"司龄开始日期"保存不生效的两层问题（日期空串转 LocalDate 抛异常、updateById 忽略 null 字段，现显式置 NULL 落库）。存量污染行（company_age_start_time=become_time）已在各租户库执行 `2026-08-30_fix_company_age_start_time.sql` 修复；指纹不匹配的个别行（如赵建礼，转正日期与司龄开始日期不等值）在详情页清空该字段即可按入职日期重算。
- 2026-08-30 田野农谷 9 项修复：用工性质高级筛选改按花名册动态字段匹配（与导出口径一致）；司龄计算三层缺陷修复（导入兜底改入职日期、Period 精确计算、离职冻结，存量修复 SQL `2026-08-30_fix_company_age_start_time.sql`）；日期筛选加 `unlink-panels`；补学历筛选；合同到期状态查询时动态纠正（零 DDL）；隐藏重复工作地点字段与"生日"列（`2026-08-30_hide_duplicate_fields.sql`，补齐 queryBirthdayEmp SQL）；员工列表加 `maxHeight` 固定表头；详情禁用态文字加深。
- 2026-08-24 部门明细追加员工级全勤开关（`full_attendance=1` 才追加）与行政经理跨公司加密导出（`EmployeeDepartmentDetailCrossCompanyExportSupport`、`CrossDomainFilter` 暴露密码头）。
- 2026-08-21 员工其他补助：BO/变更记录新增 `otherSubsidy`，`ensureEmployeeSalaryDynamicFields` 补齐第 4 个薪资动态字段，花名册模板/导入/部门明细成本全线接入；同步修复导入"姓名+身份证号"兜底匹配。同日员工合同无固定期限规则上线。

## 历史摘要
- 2026-08-19/20：部门明细导出上线（多 sheet、成本区、合同到期日、跨公司后续增强）；员工新增/编辑薪资字段（薪资等级/固定绩效/职务补助）与花名册模板插列；批量设置参保方案（按员工/按部门穿梭框）。
- 2026-06-10~16：员工编辑部门/学历保存修复、司龄/生日口径与列表驼峰字段、高级查询增强、合同新增不覆盖与期限自动计算、组织编码自动生成、花名册字段比对、花名册导入重构、子女信息、离职状态列、基础信息模板导出、合同导入、固定模板下载、按钮布局优化。
- 2026-07-17：唯一性校验统一 + 删除"是否有全勤/是否加入钉钉"表单项。更早细节见 `../archive/changelog.md`。
