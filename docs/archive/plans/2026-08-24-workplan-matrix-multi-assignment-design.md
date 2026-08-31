# 排班管理矩阵多产品多岗位修改排班设计

## 背景

排班管理矩阵当前按“员工 + 日期”打开修改排班，但后端 `queryEmployeeDayShift` 会从当天本地排班中选一条首选记录返回。前端矩阵展示又会把同一产品、同一班次签名下的多个岗位合并为 `装盒、封口` 这类文本。因此当一个员工同一天被安排到多个岗位，甚至多个生产产品时，修改弹窗容易只拿到一个产品或一个岗位，保存时再按单条记录替换，存在丢失其它岗位/产品的风险。

用户已确认：同一天不同产品或岗位可以有不同工作时间。因此设计不能假设当天所有岗位共用同一个班次。

## 目标

- 修改排班按 `员工 + 日期` 作为编辑聚合对象。
- 聚合对象内部保留多条独立分配，每条分配都能维护自己的生产产品、岗位、班次类型、开始/结束时间、白夜班和连班状态。
- 保存时以“替换目标员工当天全部排班”为语义，保证删除旧岗位、调整多产品、多岗位和改为休息/调休都不会残留旧记录。
- 第一期不改 `tbplanlist` 表结构，继续用 `ProductName` 保存生产产品、`LinkName` 保存岗位、`UserID` 保存逗号分隔员工。

## 数据结构

推荐新增员工单日明细接口，返回完整分配集合：

```json
{
  "employeeId": 102,
  "workDate": "2026-08-24",
  "userId": "manager123",
  "dayStatus": "work",
  "assignments": [
    {
      "planId": 71,
      "productMode": "standard",
      "productId": 10,
      "productName": "椰子饼",
      "positionId": 101,
      "positionName": "装盒",
      "shiftType": "custom",
      "customStart": "08:00",
      "customEnd": "17:00",
      "customShiftPeriod": "day",
      "customContinuousShift": false,
      "classId": null,
      "restShiftType": ""
    }
  ]
}
```

字段说明：

- `assignments[]` 是修改排班的核心，不再从合并后的矩阵文本或首选排班反推。
- `productId/positionId` 只用于前端回显标准产品；实际落库仍以 `productName/positionName` 为准。
- 每条分配都有独立 `shiftType/classId/customStart/customEnd/customShiftPeriod/customContinuousShift/restShiftType`，支持不同产品、岗位使用不同时间。
- 休息/调休和工作分配互斥；选择休息或调休时 `assignments[]` 必须为空，后端保存一条 `shift_type=rest` 的目标员工记录。

## 查询与展示

矩阵单元格仍可以做摘要展示，但编辑必须读原始明细：

- 单产品单岗位：显示岗位或班次，例如 `装盒`、`08:00-17:00`。
- 同产品同班次多岗位：显示 `装盒、封口`，悬浮层展示每条岗位。
- 多产品或多班次：显示 `2个产品 / 3个岗位`，悬浮层按行展示 `产品 / 岗位 / 班次 / 时间`。

修改弹窗打开时应调用新的单日明细接口，例如：

- `GET /workPlan/queryEmployeeDayAssignments?employeeId=102&workDate=2026-08-24`

旧 `/workPlan/queryEmployeeDayShift` 可以保留给小程序、申请审批、历史单排班入口使用，但矩阵修改排班不应再依赖它的首选记录结果。

## 保存语义

推荐新增保存接口：

- `POST /workPlan/saveEmployeeDayAssignments`

保存过程采用完整替换：

1. 通过 `employeeId` 解析目标员工的考勤 `userId`。
2. 查询目标日期当天所有包含该 `userId` 的 `tbplanlist`。
3. 对多人共享的旧行，只从 `UserID` 中移除目标员工并保留其他员工。
4. 对只包含目标员工的旧行，删除整条旧记录。
5. 如果提交为休息/调休，保存一条目标员工休息记录，清空产品和岗位。
6. 如果提交为工作分配，把每条 `assignments[]` 展开为一条独立 `tbplanlist`，分别写入 `ProductName/LinkName/UserID` 和对应班次字段。

这和现有单日删除、`saveAll` 中的员工当天清理策略一致，只是把输入从单条排班升级为多条分配。

## 校验规则

- 工作分配至少一条；每条工作分配必须有产品、岗位和目标员工。
- 自定义班次必须有合法 `HH:mm` 开始时间；有固定下班时间时结束时间也必须合法。
- 夜班、休息、调休不得保存连班。
- 同一员工当天允许多个产品、多个岗位，也允许同一员工重复出现在多条分配中。
- 可拦截完全重复的分配：同产品、同岗位、同班次类型、同时间、同白夜班、同连班状态完全相同时提示用户合并或删除重复行。

## 前端改造点

- `Scheduling.vue` 的 `dayShiftProductContext` 从单一产品上下文改为 `dayShiftAssignments[]`。
- 修改弹窗用表格或多行编辑器维护分配明细，每行包含产品模式、生产产品、岗位、班次类型、时间和连班。
- “添加岗位”变成“添加分配”，因为新行可能是另一个产品、另一个岗位或另一个时间段。
- 保存时构造 `assignments[]` 提交新接口；不再使用矩阵合并文本或 `queryEmployeeDayShift` 的单条结果作为保存依据。
- 矩阵摘要和 tooltip 使用原始 records 聚合，只用于展示；编辑回填必须使用后端 raw assignments。

## 后端改造点

- 新增 VO：`WorkPlanEmployeeDayAssignmentsVO`、`WorkPlanEmployeeDayAssignmentVO`。
- 新增 BO：`SaveWorkPlanEmployeeDayAssignmentsBO`、`SaveWorkPlanEmployeeDayAssignmentBO`。
- `WorkPlanServiceImpl` 新增查询完整分配、完整替换保存方法，复用现有 `findEmployeeDayPlans(...)`、`parseUserIds(...)`、`buildLocalCustomAssignments(...)`、`buildLocalRestAssignments(...)`、`persistResolvedAssignments(...)`。
- `queryEmployeeDayShift(...)` 可保持兼容，但需在注释或文档中明确它是旧的单条首选视图，不适合作为多岗位矩阵编辑数据源。

## 后续演进

第一期保留扁平表，成本最低。需要注意生产产品名称应唯一，岗位名称至少在同一产品内唯一，否则历史记录只能按名称回显。

长期如果要支持稳定 ID、历史产品改名不影响排班、单条分配审计和批量复制，可以新增规范化排班明细表，保存 `product_id/position_id/employee_id` 等稳定外键；但这会牵涉导入、导出、矩阵查询和工资/考勤统计联动，建议作为第二期。
