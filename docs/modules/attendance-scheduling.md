# 排班与单双休（排班矩阵 / 添加排班 / 自定义班次 / 排班上传 / 车间 / 单双休生成）

菜单前缀：`/workPlan*`、`/workPlanProduct`、`/attendanceData`（排班展示部分）、`/hrmWorkweekSetting`、`/workPlanApplication`。

## 需求要点
- 添加排班：新增行前必选"标准产品/自定义产品"模式；每条主行独立维护产品/岗位/人员，岗位为 `positionRows` 多子行（各自带人员，可从全员补充）；提交前展开为 `/workPlan/saveAll` 扁平数组（`productName/linkName/userId`）。
- 人员来源：展示候选来自 `hrm_employee`（`is_del=0` 且 `entry_status in (1,3,4)`），不以 `tbattendanceuser` 快照为全集；无考勤组员工 `groupId` 返回空字符串仍保留；缓存 key `*_getAllUsers_display_v3`。
- 排班管理矩阵：员工一行 + 当月日期列，全员显示（无排班也占行）；点击员工某天弹"修改排班"，编辑对象是 `员工+日期` 完整分配集合（`/workPlan/queryEmployeeDayAssignments` + `/saveEmployeeDayAssignments`，完整替换语义）；单元格支持删除（`/workPlan/removeEmployeeDayShift`，共享行只移除目标员工）；前端本地分页。
- 车间：自由文本输入，落 `tbplanlist.workshop_name`，同产品同岗位不同车间不合并；历史空值回显才用考勤组名兜底。
- 排班上传：模板 `排班日期/员工/电话/排班类型/排班时间/白班/夜班/是否连班/备注`（兼容横版日期分组与历史"手机号"列）；电话有内容按"姓名+电话"匹配、为空按姓名匹配；`休假/休息` → `rest_shift_type=rest`，`调休` → `adjust`；同员工同日重复行拒绝；导入无 id 时按 `WorkDate+UserID` 复用旧记录，保留共享行其他员工。
- 连班：只有白班自定义排班可连班；员工档案 `is_continuous_shift` 自动带出，Excel/界面显式值优先（`customContinuousShiftExplicit`）。
- 单双休生成：自然周（含 1/1 为第 1 周），单休=周日、双休=六日，按单/双休交替生成全年；改某周可选"仅修改本周"或"向后重算"（`recalculateFollowing`）；月度日历按"已保存日级设置 > 调休上班 > 法定休息 > 周休"判定，保存只 upsert 提交日期；节假日表为空时用内置 2026 兜底。

## 设计与契约
- `tbplanlist` 扁平事实表：`ProductName`=产品、`LinkName`=岗位、`UserID`=逗号拼接考勤用户、`shift_source`(standard/custom/rest)、`custom_shift_id` → `hrm_workplan_custom_shift`、`custom_start/end/cross_day/shift_period`、`rest_shift_type`、`custom_continuous_shift`、`workshop_name`。与小程序生产排班共用此表（2026-08-30 统一）；2026-09-06 起也是加班/夜班统计的排班唯一事实源（见 `overtime.md`）。
- 生产产品配置三表：`hrm_workplan_product / _product_position / _position_employee`（`/workPlanProduct/queryTree|saveProduct|savePosition|sortProducts|sortPositions|delete*`，映射菜单 `/hrm/attendance/workplanProduct`）。
- 标准班次提交走钉钉任务化：`/workPlan/saveAll` 返回 `taskId` → `/workPlan/querySubmitProgress`（内存态，重启丢失）；后端最多重试 10 次，指数退避 200ms→30s；错误按"行+员工"中文输出。自定义班次仅本地落库不推钉钉。
- 展示查询只读本地快照（`hrm_attendance_plan/group/shift`、`tbattendanceuser`），不实时调钉钉；提交链路缓存 10 分钟、展示缓存 30 分钟，调组时主动清理。
- 单双休表：`hrm_workweek_setting`（年+周次）、`hrm_workweek_day_setting`（年+日期）；接口 `queryYearSettings/initYearSettings/updateWeekType/queryMonthCalendar/saveMonthCalendar`。

## 近期变更
- 2026-09-07：排班管理新增"批量设置休息"——点击日期矩阵单元格弹窗二选一（休息/继续排班），选休息调 `/workPlan/batchSetRestDay` 将生产体系月休四天的所有在职员工当日设为休息；小程序端同步。
- 2026-09-07：排班申请审批菜单补齐——所有租户库（hr_0001~hr_0006）执行菜单补丁SQL，`seed-data.sql` 同步更新新租户开通流程；菜单路径 `/hrm/attendance/workPlanApplication`，接口权限 `/workPlanApplication` 映射到排班管理菜单。
- 2026-09-05：排班管理"修改排班"产品/岗位/车间改为非必填——前端 `Scheduling.vue` 移除产品组数量、产品名称、岗位数量、岗位名称的验证；后端 `WorkPlanServiceImpl.buildPlanFromEmployeeDayAssignment` 移除"生产产品不能为空"和"岗位不能为空"校验。车间字段本身已无验证。

## 历史摘要
- 2026-08-23：排班添加与单元格编辑产品模式（标准/自定义产品双轨，休息/调休免产品岗位）；岗位多行 `positionRows` 与全员补充选人。
- 2026-04-12：自定义班次后端实现（saveAll 任务化、`hrm_workplan_custom_shift`、queryCustomShiftList、缓存拆分）；早期"自定义班次同步钉钉"设计已废弃。
- 2026-05-30/31：白/夜班别（`custom_shift_period`）、Excel 模板口径、排班管理只读列表→日历矩阵→员工日期矩阵演进、全员显示、关键字、导出。
- 2026-06-01~04：调休/休息/连班（`rest_shift_type`、连班归一化）、上传迁移到排班管理、模板下载按月重写、横版 workplan.xls、同日同名覆盖规则、ResultSet 修复（rest_shift_type 补列）。
- 2026-04-06/06-03/06-17：每周单双休生成、后续变化与节假日统计、月度日历配置与 2026 兜底。2026-07-25：连班自动带出与导入电话匹配。
- 更早细节见 `../archive/changelog.md`。
