# 考勤「审批数据-获取审批数据」跨月抓取丢数据 · 设计评审

> 评审日期：2026-09-04 ｜ 状态：**待用户确认后再动工**
> 关联：`HrmAttendanceApprovalSyncServiceImpl` / `HrmAttendanceApprovalServiceImpl` / `HrmAttendanceApprovalController` / 前端 `views/hrm/attendance/approval/Index.vue`
> V4 定稿：弹窗含“开始日期”选择器（用户自选起点，最早=今天−3个月）→ 点确定 → **终点=点击确定的“当下时刻”** → 拉取 [开始日期, 点确定当下] 已通过审批，全量落库+按业务日归类；**钉钉考勤(打卡)本次不纳入**。原 V3 的“起点自动取当下−3月”被本稿的“用户自选开始日期”取代。

---

## 一、Bug 复述

PC「考勤管理 → 审批数据 → 获取审批数据」，按业务月份抓审批。用户反馈两种场景在“获取 8 月”时拿不到：

1. **提前申请**：某员工 7 月申请了 8 月的年假（发起月 7、业务月 8）。
2. **滞后补录**：某员工 9 月初才补卡 8 月某天的缺卡（发起月 9、业务月 8）。

根本诉求：**“获取某月数据”应按审批的业务日期（实际发生月）归类，与审批的发起/申请月份无关。**

---

## 二、根因定位（已确认）

抓取链路：`fetchMonthData → HrmAttendanceApprovalSyncServiceImpl.fetchMonthData → fetchProcessInstanceIds(钉钉 topapi/processinstance/listids)`

关键代码（`fetchProcessInstanceIds`）把钉钉查询窗口设为目标月份 1 日 ~ 末日：

```java
request.setStartTime(month.atDay(1)....toEpochMilli());
request.setEndTime(month.atEndOfMonth().atTime(23,59,59)...toEpochMilli());
```

随后对每条审批再按业务日期过滤：

```java
if (!isApprovalInTargetMonth(entity, month)) continue;  // 业务 beginTime 必须在目标月
```

**矛盾点**（已用钉钉官方文档核对 `topapi/processinstance/listids`）：
> start_time/end_time 是「审批实例**发起时间**」，示例即“获取审批单发起时间在某区间内的审批单”，**并非业务/工作日日期**。

因此整个流程是：**按发起月拉回 → 又按业务月丢弃**。只有「发起月 == 业务月」才同时命中。于是：
- 拉 8 月 → 发起不在 8 月的查不回来；
- 拉 7 月/9 月 → 能查到，但业务月在 8 月 ≠ 7/9，被 `isApprovalInTargetMonth` **永久丢弃**（不入库）。

**佐证整表语义按业务月**：mapper `queryPageList` 用 `date_format(a.beginTime,'%Y-%m-%d') between ...` 过滤月份；重抓前 `deleteStaleMonthDataForSelectedEmployees` 也是按 `beginTime` 月界清旧。整条功能都按“业务 beginTime 归属月”组织，唯一错的是上游枚举按发起月。

---

## 三、为何不能简单“扩大时间窗”（用户已否决）

员工**可能 2 月申请、8 月才发生业务**，发起时间与业务时间无可靠上限。任何“±N 月 / 120 天”的窗口都只是缓解，不能根治，且提前量不可枚举。

---

## 四、备选方案与取舍

### 方案 A：放弃 process-instance 流程，改用 `topapi/attendance/getupdatedata` 按工作日拉
- 思路：对目标月**每员工 × 每个工作日**调一次考勤更新接口，`approve_list` 天然只含该实际工作日的审批（含 biz_type/tag_name/sub_type/duration/proc_inst_id/work_date），彻底无跨月/提前量问题；与现有「同步考勤」同一数据源。
- **致命约束**：
  1. 该接口是**单用户 × 单天**调用（userid + work_date 均必填），整月 = 员工数 × 31 天。
  2. **计入钉钉 Open API 月度配额**（标准版约 1 万次/月）。以本地 hr_0001≈290 人为例，一次“获取全部员工某月”≈ 290×31 ≈ **9000 次**，单次操作就几乎耗尽整月额度 —— 这正是当初 2026-05 系列方案特意选用 workflow 流程接口并做“最小化钉钉调用”优化的原因。
- 结论：**作为主通道不可取**，仅可作为补救核对手段。

### 方案 B（推荐主方案，V4 定稿）：用户选开始日期 + 点确定(终点=点击当下) + 全量落库、按业务月展示

**为什么窗口终点/起点不能由后端猜：**
钉钉 `listids` 只能按**发起时间**查。真正的痛点有两类，方向相反：
- **滞后补录**：补卡单 9 月发起、8 月业务 → 终点必须晚于 9 月补卡单发起，写死“8 月末”必然漏。
- **提前申请**：假条 2 月发起、8 月业务 → 起点必须早于 2 月，写死“目标业务月起点”必然漏。

任何“后端猜 K/猜 N 天”都不可靠（提前量/滞后量无固定上限）。正确做法：**起点由用户用日期选择器自选**（覆盖提前申请），**终点=点“确定”的当下**（覆盖滞后补卡，因现场先让员工补完卡再点）。

**核心动作：**
1. **交互（已确认）：弹窗内含“开始日期”选择器，用户必须选一个开始日期作为发起时间起点；再点“确定”→ 以点击确定的“当下时刻”为抓取终点。** 拉取窗口 = [用户选的开始日期, 点确定的当下时刻]。
   - 起点**可选范围上限 = 今天 − 3 个月**（防乱点烧 API）：开始日期不得早于该上限，早于则拒绝并提示放宽（做成配置 `hrm.approval-fetch.max-months-before=3`）。
   - 终点 = 用户点“确定”的当下（前端点确定时取当前时间戳传入），不手填。
   - 因现场“先让员工补完卡再点确定”，所有滞后补卡单发起 ≤ 该当下 → 必被覆盖；用户把开始日期选到想要的最早提前发起日之前 → 提前申请被覆盖。
2. **取消 `isApprovalInTargetMonth` 的“业务日期非目标月即 continue”**，改为：凡审批**已通过**（COMPLETED+agree）一律按 `proc_inst_id` 幂等落库、保存**真实业务日期**。查询/统计页本就按业务 `beginTime` 过滤，落库与展示解耦。
   - 关键副作用：终点=点确定当下，会拉回少量“业务月在未来的”审批（如 9 月点、有人请了 10 月假）。正因“全量落库按业务日归类”，它落进 10 月、不污染 8/9 月视图。
   - 该步同样兜住“历史某月发起的单被丢弃过”的存量漏数据：只要发起日在用户选过的范围内被拉回过一次，即永久在库、按业务日进对应月的统计。
   - 成本：**0 新增钉钉调用**（只是不再丢弃已拉回的数据）。
3. 清库口径随“按业务日展示”收紧为“按本次窗口拉回、且业务日落在用户要看的月份才计入”，见 §五.4 / §六。

**对两类 bug 的覆盖（示例：员工 8 月考勤，月末到次月初补完卡，操作者此时点“获取”→ 选开始日期 → 点确定）：**
- 滞后补录（9 月发起 8 月业务）：终点 = 点“确定”的当下（≥9 月补卡单发起）→ 拉回，按业务日 8 月落库、进 8 月展示。
- 提前申请（如 6/10 发起、8 月业务）：用户把开始日期选到 ≤6/10（在 3 个月上限内）→ 拉回，按业务日 8 月落库。若用户需要覆盖更早的提前发起，选更早开始日期（上限 3 个月）；个别 >3 个月极少，靠“已全量落库”历史单兜住或单员工单独补抓。

> 一句话：**弹窗选“开始日期”（起点，最早=今天−3个月）→ 点确定 → 以 [用户选的开始日期, 点确定的当下] 拉取已通过审批，全量按业务日幂等落库、按业务月展示。** 终点=点击当下解决“滞后补录”，起点=用户自选开始日期（≤3个月内护栏）解决“提前申请”，全量落库解决“发起月≠业务月被永久丢弃”。

---

## 五、遗留不确定点（实施前需你确认/或由一次小验证核对）

0. **方向确认（V4 定稿）**：弹窗含“开始日期”选择器（**起点=用户自选开始日期**，最早=今天−3个月）→ 点确定 → **终点=点击确定的“当下时刻”** → 拉取 [开始日期, 点确定当下]。范围边界：本次**只处理“审批数据”获取，钉钉考勤（打卡/考勤数据）不在本次范围，先不管**。

1. **操作节奏与默认**：按你“月末~次月初先让员工补完再获取”的节奏，点确定时所有滞后补卡单已发起 → 终点=点击当下自然覆盖；起点由用户在开始日期选择器里选到想覆盖的最早提前发起日（≤3个月上限）；默认可给“今天−3个月”。
2. **提前请假的合理上限**：年假等一般提前多久发起？已由你拍板**开始日期最早 = 今天−3 个月**（§五.7 预算据此）。钉钉 listids 单次跨度 ≤120 天需分窗，起点 ≤ 当前 365 天。
3. **终点取值确认（已拍板）**：点“确定”的当下时刻为终点（前端点确定时取当前时间戳传给后端），不手填。滞后的补卡单发起 ≤ 该当下 → 必被覆盖。
4. **`isApprovalInTargetMonth` 去掉后**，当前重抓清理逻辑 `deleteStaleMonthDataForSelectedEmployees` 用 `beginTime` 月界删“非 retained 记录”——若改为全量落库，清理口径需改为“该员工该流程模板、业务 beginTime 落在看板所选业务月、且不在本次 retained 集合”的删除，避免误删其他月提前入库的记录（详见 §六改造点 5）。这一点必须谨慎设计，是主要回归风险。
5. **终点=点“确定”当下会拉回“业务月在未来的”审批**：落库会增长、接口调用略增（未来业务月数据入库量可接受，靠按业务日归类不污染当前视图，需业务确认）。
6. **幂等保障（已核对代码，非风险点）**：`tbattendanceapprove.id` 即钉钉 `proc_inst_id`（`@Id` 主键，parser `entity.setId(processInstanceId)`）；落库走 `approvalRepository.findById(procInstId)` + `save()`（同 id 为 merge 更新非新增）。因此**任意窗口重叠（如 9/5 拉 [8.1~9.5]、10/5 拉 [9.1~10.5] 都命中同一张 9 月审批单）都不会产生重复行**，只会更新同一行。本项由测试“同 procInstId 重复窗口仅一行/更新不增行”覆盖（§六）。
   - 唯一需防的是 §五.4 的**清理误删**（`deleteStaleMonthDataForSelectedEmployees` 若按“业务月整段删非 retained”会误删他业务月提前入库的行），与“插入重复”是两回事，务必分开设计。
7. **API 调用量预算与“开始时间最多往前 X 个月”上限（防乱点爆配额，含成本模型）**

   **现有单次 fetch 的调用成本模型**（已核对代码，串行 + 每呼后 `sleep(100ms)` ≈ 上限 10 QPS）：
   - 每人 1 次 `topapi/process/listbyuserid`（解析可见流程模板）；
   - 每模板每分页(20/页) 1 次 `topapi/processinstance/listids`；
   - **每张审批单 1 次 `topapi/processinstance/get`（详情）—— 这是成本主体，且随“时间范围变宽 → 拉回单量变多”近似线性增长**。

   记 **E=员工数、P=每员工匹配的流程模板数、m=范围内每人平均单量**：
   `总调用 ≈ E×P×ceil(m/20) [listids] + E×m [get] + E×1 [listbyuserid]`。
   以 hr_0001≈290 人、范围宽到 X 个月、每人月均 ~1.2 单（请假+加班+补卡低频）估算，m ≈ 1.2X：
   - **X=3 个月（上限，由你拍板）** → m≈3.6 → `get` ≈ 290×3.6≈1040 + listids ≈ 290×P×1 页 + listbyuserid 290 → 总量 ~1500~2200 次，100ms 串行约 3 分钟内完成，QPS 下安全。
   - 公式可随真实员工数/月单量重算：如放开到 X=6 → ~2800~3600 次、约 5~8 分钟；X=12 → ~6000+ 次（且 120 天子窗口分片使 listids 翻倍）。

   **结论与建议**（workflow 系接口为 QPS 限额，非 attendance 的月度单位配额；但总量大仍拖慢任务并接近频控风险）：
   - **上限由你拍板收紧为 X=3 个月（默认且硬顶，作为用户可选“开始日期”的最早值）**：一次“全部员工”整段 3 个月 ≈ 1500~2200 次调用，100ms 串行约 3 分钟完成，QPS 下很安全。按你现场“月末~次月初先补完再获取”的节奏，补卡滞后由终点(点确定当下)覆盖；提前申请由用户自选的开始日期覆盖（最早=今天−3月）。提前>3 个月的极少见，必要时按单员工单独补抓。
   - **配套防乱点护栏**：① 范围内单员工空档不新增调用（listids 返回空即停）；② 对“全部员工 + 大范围(>2 个月)”提示耗时预估或二次确认；③ 建议按员工维度记录上次 fetch 时间，短时间内(如当日)重复同范围直接复用/拒绝，避免反复点刷爆量。
   - 上限做成配置项 `hrm.approval-fetch.max-months-before`（**默认 3**），并按真实员工数与月单量校准上述估算。若未来确需放开，再调大该配置并复核预算。

---

## 六、预期改造点（评审通过后实施）

**前端（hr_web）**
1. `views/hrm/attendance/approval/Index.vue`：把“获取审批数据”弹窗改为含**“开始日期”选择器**（用户必选，作为发起时间起点；最早可选=今天−3个月）＋“确定”按钮；点“确定”→ 取**点击时刻**为 `fetchEndTime`、以**用户选的开始日期**为 `fetchStartTime` 发起；未选开始日期或早于上限时前端拦截提示。原“按业务月选择”仅保留在列表视图作筛选，不用于抓取。
2. `approval-fetch-utils.js` 的 `buildApprovalFetchPayload`：改为携带 `fetchStartTime`/`fetchEndTime`（毫秒时间戳；`fetchStartTime`=用户选开始日期、`fetchEndTime`=点确定时刻）；`api/hrm/attendance/approval.js` 不变（沿用 fetchMonthData）。

**后端（hainan）**
3. `AttendanceApprovalMonthBO`：新增可选 `fetchStartTime`、`fetchEndTime`（Long 毫秒，代表**发起时间窗口**，`fetchStartTime`=用户选开始日期、`fetchEndTime`=点击确定当下）。为向后兼容保留 `month`（老客户端不传范围时，仍按旧口径兜底）。范围=null 或非法时拒绝/回退并给出清晰提示。
4. `HrmAttendanceApprovalSyncServiceImpl.fetchMonthData`：
   - `fetchProcessInstanceIds` 由“目标月 1 日~末日”改为**用发起窗口 [fetchStartTime, fetchEndTime]**，并按 ≤120 天子窗口分片枚举；
   - **服务端护栏**：`fetchStartTime` 不得早于“点确定时刻 − max-months-before 个月(默认3)”，超限拒绝并在前端提示放宽（可配置 `hrm.approval-fetch.max-months-before=3`）；
   - 去掉“业务日期不在目标月即 continue”的硬丢弃，改为**幂等落库全部已通过审批**（保留 `preserveManuallyEditedSubtype` 与 `retainedApprovalIds` 语义）；
5. 清理口径：`deleteStaleMonthDataForSelectedEmployees` 从“业务月整段删除”调整为“该员工×流程模板×业务 beginTime 落在本次看板所选业务月、且不在本次抓回集合内才删”，防止把提前/滞后人库的他月记录误删（与 `fetchMark` 判重口径一并复核）。
6. `fetchMark` 语义：目前按「月份×员工×类型」记完成标记。改“全量落库 + 按窗口拉取”后，标记调整为“该员工×审批类型已在某发起窗口内处理过”，避免重复同窗口重复全量拉；但不要因标记存在而跳过“补录滞后单”所需的新窗口（开始日期/点确定时刻不同 → 视为新窗口）。
7. controller 编排基本不变（仍为窗口触发 + 进度 + 互斥锁 + 自动重试）。

**测试**
- `HrmAttendanceApprovalSyncServiceImplWorkflowTest`：新增“业务日≠发起范围所属月仍应入库”“范围终点=点确定时刻拉回次月补卡”“范围起点(≤3月)拉回提前申请”“重抓清理不误删他业务月记录”“未来业务月审批落库但不污染目标月展示”“同 procInstId 在重叠范围重复拉到仅一行、只更新不增行”“起点早于护栏拒绝/提示”用例；改动/新增“业务日期越界跳过”测试的断言方向。
- 回归：`mvn -Dtest=HrmAttendanceApprovalServiceImplTest,HrmAttendanceApprovalControllerTest,HrmAttendanceApprovalSyncServiceImplTest,HrmAttendanceApprovalSyncServiceImplWorkflowTest,HrmAttendanceApprovalProcessInstanceParserTest -DfailIfNoTests=false test`

**文档**
- `hainan/docs/development.md`（三端契约/模块说明）补充“终点=点确定当下、起点=当下−≤3月、全量按业务日归类”语义及 `fetchStartTime`/`fetchEndTime` 契约字段。
- 按硬性约定，若涉及小程序/PC 对外契约变化同步对应文档；本次改动 PC 抓取契约（新增可选范围字段，向后兼容），须在 `miniapp/docs/完成情况.md` 记录本轮。

---

## 七、验收方式（建议）

本地 29081 实例 + 真实租户库，定向构造/核对：
- 在钉钉侧分别造「提前发起 / 业务落目标月」与「次月补录 / 业务落上月」两条已通过审批；
- 选一个覆盖上述两单发起的范围点“获取” → 两批都应出现在各自业务月的列表与统计中；
- 幂等：重复同范围不重复插入；对同一 proc_inst_id 更新不产生孤儿数据；
- 回归：跨月记录不污染相邻业务月视图；起点早于 3 个月护栏时被拒绝/提示；
- 数据核对后用 SQL 清理测试残留，避免污染生产统计。

---

## 八、请你拍板

已定：
- **起点（用户自选开始日期）**：弹窗含“开始日期”选择器，用户必须选开始日期作发起时间起点；最早可选 = 今天−3 个月（配置 `hrm.approval-fetch.max-months-before=3`）。
- **终点（点确定=当下）**：点“确定”→ 终点 = 点击确定的当下时刻。
- **范围边界**：钉钉考勤（打卡/考勤数据）本次**不纳入**，先不管。
- **幂等**：`id=proc_inst_id` merge 落库，重叠窗口不产生重复行（已核对代码，非风险）。

待你最终确认 1 点即可动工：
1. 是否认可上述“选开始日期（最早=今天−3月）→ 点确定（终点=点击当下）”的最终交互？（会连带拉回少量未来业务月审批，靠按业务日归类不污染当前视图）认可即按 §六 实施并补测试与文档（清理口径按 §五.4 重点处理）。

确认后我按 §六 实施并补测试与文档。

---

## 九、实施完成记录（2026-09-04，V4 已落地）

按 §六 实施的最终取舍与落地明细（**含与 §六草稿的两处偏差**，以本节为准）：

**核心落地：月/窗双入口**
- 老入口 3 参 `fetchMonthData(month, empIds, types)` 与定时任务 = **月模式**：requestFetch 字段保持 null → 基类 `fetchProcessInstanceIds` 用 month 窗口、主循环**仍按业务月过滤**（保留 `isApprovalInTargetMonth`）、收尾**仍按 month 边界 stale 清理**——行为与改造前完全一致（既有 23 个 workflow 测试保持全绿）。
- 前端手动 = **窗口模式**：5 参 `fetchMonthData(month, start, end, empIds, types)` 仅当 start/end 至少一个非 null 才进入——写 `requestFetchStartTime/EndTime` 实例字段 → 基类 listids 按 [start,end] ≤120 天分片（`fetchProcessInstanceIdsInWindow`）→ 主循环**跳过业务月过滤、已通过审批全量幂等落库**（跨月单不被丢）→ 收尾**跳过整段 stale 清理**。月/窗以 requestFetch 字段是否为 null 区分，`doFetchMonthData` 内按此分支。

**偏差 1（vs §六.5）**：窗口模式清理口径未做成"按业务 beginTime∈看板业务月∧不在 retained 才删"，而是**窗口模式直接跳过整段 stale 清理**（`deleteStaleMonthDataForSelectedEmployees` 开头守卫 return）。原因：`tbattendanceapprove` 表**无"审批发起时间"列**，无法安全表达"只删窗口内未回库的单"；若仍按业务月整段删非 retained，会误删"发起早于窗口起点但业务落目标月"的既有跨月单（正是本 bug 要保护的）。缺失/撤销单由"同步考勤 / 人工核对"路径兜底，不靠窗口拉取做强清理。月模式不受影响、仍按 month 边界删。

**偏差 2（vs §六.6 fetchMark）**：本次**未改 fetchMark 语义/去重口径**（仍按 月份×员工×类型 记完成标记；窗口拉取天然幂等、不增行，短期重复窗口靠用户操作频率克制 + 进度互斥锁兜底）。§六.6 的"窗口级去重标记"列为后续可选项，未在本轮引入以免扩大改动面。

**新增/变更文件**
- 后端 `HrmAttendanceApprovalSyncServiceImpl`（月/窗双入口、`doFetchMonthData` 抽取、`fetchProcessInstanceIdsInWindow` 分片、`updateFetchProgress` 对 `progressTracker` 加 null 守卫修复测试空指针）；`HrmAttendanceApprovalServiceImpl.fetchMonthData(queryBO)` 解析 BO 窗口透传 5 参；`AttendanceApprovalMonthBO` 新增 `fetchStartTime`/`fetchEndTime`；接口 `IHrmAttendanceApprovalSyncService` 新增 5 参方法（保留 3 参）。
- 前端 `approval-fetch-utils.js` `buildApprovalFetchPayload(month, empIds, types, {fetchStartTime, fetchEndTime})`；`Index.vue` fetch 弹窗新增「开始日期」date-picker（`required` 必选，disabled 今天前 3 月与未来、含操作提示文案，label 为「开始日期」）、`confirmFetchMonth` 未选开始日期则**拦截提示必须选择**（不设默认回退，杜绝"默认=业务月首日仍漏提前单"的体验坑）并传 start(该日 00:00)/end(Date.now())。
- 测试：`HrmAttendanceApprovalSyncServiceImplWorkflowTest` 新增 `fetchMonthData_windowMode_shouldPersistCrossMonthBusinessApproval`（窗口入口 + 业务落 3 月、目标 4 月 → 必入库）。**24 tests 全绿**。
- Controller 无需改（BO 透传）。

**验证**：`mvn compile -DskipTests` 通过；`mvn -Dtest=HrmAttendanceApprovalSyncServiceImplWorkflowTest test` = 24 tests / 0 failures / 0 errors。
**待办（已办）**：前端经 SFC 语法校验；用户已登录 28080 点验通过（详见下方 §九·补）；**本轮未触碰 /mp/* 小程序接口，不涉小程序契约表，无需同步 `miniapp` 文档**（此处早前"完成情况.md 需补记录"为 PC 功能误挂，撤回）。

### §九·补：V4 前端交互三次收敛（用户验收定稿，2026-09-04 03:15~03:49）
按用户现场点验反馈对 §九「前端」描述的逐步修正，**以本节为准**：
1. **「开始日期」强制必选（03:15）**：去掉"默认=目标业务月首日"的兜底——防止操作员不填时默认当月 1 号导致提前申请的跨月单仍漏。`Index.vue` 开始日期 el-form-item `required`，未选即拦截提示（示例：7 月申请 8 月年假→开始日期选 7 月初；9 月补卡 8 月→选 8 月）。
2. **删弹窗「选择月份」下拉（03:33）**：用户反馈弹窗里"选择月份 + 开始日期"两个日期控件冲突。定稿：弹窗只留「开始日期」，目标业务月(month)不再在弹窗选，改为**从列表页顶部已有月份筛选 `state.form.date` 派生**——有值用它、无值默认当前系统年月往前一个月（新函数 `resolveFetchTargetMonth()` 返回 YYYY-MM）。后端 parseMonth/窗口逻辑完全兼容，**无需改后端**。`fetchMonth` ref 引用全清。
3. **前置"补卡/补假提交完毕"确认框（03:44）**：点「获取审批数据」按钮**先弹** `ElMessageBox.confirm`（文案：是否已确认所有员工补卡/补假申请都已提交完毕；按钮「已全部提交，继续」/「暂不获取」），确认后才打开「开始日期」弹窗。此确认框点「继续」即代表现场"补录已收口"的边界。
4. **结束时间边界上提到确认框时刻（03:56，用户要求"一起改"）**：`fetchEndTime` 不再在第二步弹窗点「确定」时才取 `Date.now()`，而是在确认框点「已全部提交，继续」的当下即锁定（新增 `lockedFetchEndTime` ref，`openFetchDialog` confirm 通过后 `lockedFetchEndTime.value = Date.now()`），`confirmFetchMonth` 用该锁定值作为窗口终点（守卫：未锁定则回退当前时刻）。即**结束时间边界 = 用户确认"补录已收口"的点击时刻**，与用户原初"点『已全部提交，继续』才是结束时间边界"的诉求完全对齐；点继续之后（直到点弹窗确定）新补进来的单不再属本次范围。
5. **验收结论（03:49~03:56）**：用户首次点验未见确认框系浏览器缓存旧页面，硬刷新后确认框效果正常；随后要求把结束时间边界上提到确认框时刻，已改并经 SFC 校验 + Vite 热更新，**V4 交互验收通过**。
