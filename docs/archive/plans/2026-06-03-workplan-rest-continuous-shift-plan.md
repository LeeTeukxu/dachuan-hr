# Task Plan: 排班连班与休息类型（2026-06-03）

## Goal
在现有排班功能上新增：
- 自定义排班类型支持“休息”。
- 只有白班自定义排班可选择“连班”。

## Constraints
- 遵循现有标准/自定义排班双轨设计。
- 不破坏排班导入、排班管理矩阵和打卡/加班统计读取班次的兼容性。
- 先写失败测试，再改生产代码。
- 更新 `docs/requirements.md` 与 `docs/development.md`。

## Phases
1. 梳理排班数据流 - complete
   - 已确认后端存在 `rest` 基础分支，连班字段尚未发现。
2. 补红灯行为测试 - complete
3. 实现后端排班扩展 - complete
4. 实现前端交互展示 - complete
5. 更新项目文档 - complete
6. 运行聚焦验证 - complete

## Decisions
- 使用 `shiftType=rest` 持久化休息排班，界面统一显示“休息”，旧“调休”文本仅作为兼容输入。
- 使用 `customContinuousShift` 记录连班，只有白班自定义排班可保留 `true`。
- 本地自定义班次唯一性纳入 `continuous_shift`，防止连班与非连班复用同一事实班次。

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| work-plan-utils 测试补丁上下文不匹配 | 第一次补红灯测试 | 按实际测试文件行号重新定位后再补 |


## Verification
- `mvn -Dtest=WorkPlanListControllerTest,WorkPlanServiceImplTest test`：39 tests, 0 failures/errors。
- `node tests/work-plan-utils.test.mjs && node tests/work-plan-api.test.mjs && node tests/work-plan-scheduling-records.test.mjs && node tests/work-plan-records-page.test.mjs && node tests/clock-overview-utils.test.mjs && node tests/work-plan-time-picker.test.mjs && npm run build`：通过；构建仍有既有 `::v-deep` 过时提示和 chunk size warning。
