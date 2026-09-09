# 通用「断点续传」组件（ResumableJobCheckpoint）设计评审

> 设计日期：2026-09-09 ｜ 状态：**已定稿落地**（组件+审批接入+通知中心「继续」按钮 已完成编译）
> 关联后端：`common` 包 `ResumableJobCheckpoint`(已建) ｜ `HrmAttendanceApprovalSyncServiceImpl` / `HrmAttendanceApprovalServiceImpl` ｜ `HrmAttendanceApprovalController` ｜ `HrmAttendanceDataController` / `HrmAttendanceDataServiceImpl` ｜ 前端 `NotificationCenter.vue` / `NotificationList.vue`
> 关联文档：`docs/modules/attendance-sync.md`、`docs/modules/attendance-scheduling.md`

---

## 一、背景与目标（一句话）

把「批量同步/抓取任务失败后，**能接着上次跑，不从头再来**」这件事抽成**公用的可复用组件**，审批获取先接入；以后考勤同步、任何逐员工/逐条目的同步类功能都能几行接入。

### 用户原话还原
- 触发场景：审批获取/同步考勤拉了 400 多条、拉了很久，**中途服务器/网络断了** → 不希望用户**重头白等**，想从断点接。
- 真实动机：**为省用户等待时间 / 免重跑麻烦**，**不是**为省钉钉 API 配额。
- 诉求：把断点续传**抽成公用方法**，以后别处类似功能可共用。

### 关键现状盘点（重要，防重复造轮子）
| 功能 | 现状 | 说明 |
|---|---|---|
| 员工管理「同步考勤」 | **已有**完整按员工断点续传 | `HrmAttendanceDataServiceImpl`：7步流程，Redis 存「已处理员工集合 + 本次参数指纹」，`SyncData` 主入口自动判断失败+指纹一致→从断点恢复；`SyncDataWithAutoRetry` 第2/3次自动重试即续传。 |
| 审批「获取审批数据」 | **没有**断点续传 | `HrmAttendanceApprovalSyncServiceImpl.doFetchMonthData` 单一大循环(员工→模板→实例)，无已处理员工持久化，失败只能重头全拉。 |

> **结论：审批是真正缺的。考勤不用改，本次只抽组件 + 审批接入，最保险。**

---

## 二、现有可复用底座（不动它们）

- `common/Redis`：现成的 Redis 简易封装（setex/del/get/keys…）。断点存 Redis，天然跨实例/重启可用。
- `common/ProgressTracker`：已是审批+考勤**共用**的进度组件，但**只存百分比/status/message，不存「已处理到哪」**。
- `common/MonthlyFullSyncGuard`：`common` 包放独立 `@Component` 通用组件的**先例**（本设计同风格）。

---

## 三、组件设计（核心）

### 3.1 职责定位
`ResumableJobCheckpoint` = 「批处理任务的断点登记簿」。它**不关心业务数据长什么样**，只负责：登记任务指纹、登记已完成的工作项集合、判断能否续传、清理断点。**单条已入库数据幂等由各业务自己保证**（审批已按 proc_inst_id 幂等落库）。

> 复用边界（关键）：
> - 组件只管「断点在哪、续不续」；
> - 「每个工作项干完后幂等写入 DB」仍由业务循环负责；
> - 「撤销/失败清理整段 stale」逻辑不动（审批只在成功跑完后做整段清理，与续传不冲突）。

### 3.2 存储模型（Redis）
每个「任务」一组 key，按**任务实例 key = 前缀 + 租户(companyId) + 任务名(biz)** 隔离：

| key（拼接后） | 值 | 含义 |
|---|---|---|
| `<biz>:<companyId>:fingerprint` | 字符串 | **任务指纹**：唯一标识「本次任务是什么」。审批=员工集合指纹+月份+审批类型+窗口；考勤=员工集合+起止日期。用于判断两次运行是否同一任务。 |
| `<biz>:<companyId>:processed` | CSV(工作项id) | **已完成工作项集合**（审批=已处理员工id；可扩展多 slot 支持考勤多阶段）。 |
| `<biz>:<companyId>:status` | RUNNING/FAILED/SUCCESS | 上次运行收尾状态（异常置 FAILED、完成置 SUCCESS）。 |

> 前缀建议统一 `hrm:job:resume:`；全部 `setex` 带 TTL（默认 24h，防陈旧断点卡死），可配置。

### 3.3 对外接口（草案，Java）
```java
@Component
public class ResumableJobCheckpoint {
    /** 开启/继续一个任务：返回本次应从哪个工作项集合继续（null=从头）。 */
    ResumeContext begin(String biz, String companyId, String fingerprint, Collection<?> allItems);

    /** 登记一个工作项已完成（追加进 processed）。 */
    void markProcessed(ResumeContext ctx, Object item);

    /** 整轮成功：置 SUCCESS（可选保留指纹便于查“上次拉过没”）。 */
    void complete(ResumeContext ctx);

    /** 整轮失败(异常)：置 FAILED，保留 processed 供下次续传。 */
    void fail(ResumeContext ctx);
}
// ResumeContext = { biz, companyId, fingerprint, remainingItems(未完成), completedSet(已完成) }
```

**续传判定规则（与考勤现有一致语义）**：
- 读 status + fingerprint：
  - `fingerprint` 与本次**不一致**（换了任务/换了人/换了日期/换了审批类型）→ **从头**，重置 processed；
  - `fingerprint` 一致且 `status ∈ {FAILED, RUNNING}` → **续传**：`remainingItems = allItems − completedSet`，从 remainingItems 继续；
  - `status == SUCCESS`（上次已完成）→ 默认**从头重拉**（用户主动再点=想刷新），重置；除非调用方要求 SUCCESS 也跳过（可选参数，审批暂用默认“从头”）。
- Redis 读写异常不阻断业务（catch 后当无断点从头跑，安全兜底）。

---

## 四、审批侧接入方案（本次落地）

### 4.1 改动点（改动收敛、风险可控）
文件：
1. `common` 新增 `ResumableJobCheckpoint.java`（+ 内部 `ResumeContext`）。
2. `HrmAttendanceApprovalSyncServiceImpl.doFetchMonthData`：
   - **入口处**：用「业务月/窗口 + 员工集合 + 审批类型」拼 fingerprint → `begin(...)` → 得到 `remainingEmployees`，**员工主循环只遍历未完成员工**。
   - **员工循环内**：每处理完一个员工（该员工下所有模板/实例都幂等落库完成）→ `markProcessed(该员工)`。**注意：员工循环内断点采集要放在“真正完成该员工全部处理”之后**，与现有 `completedUserIds` 对齐。
   - **整轮成功返回前**：`complete(...)`。
   - **异常路径**：方法 finally/外层 catch 置 `fail(...)`（若已在 `doFetchMonthData` 抛给上层）。
3. `HrmAttendanceApprovalServiceImpl.fetchMonthData`：异常 catch 处（已标 FAILED 进度处）同步触发组件 `fail`，保持进度与断点状态一致。
4. `HrmAttendanceApprovalController`：**无改动**（锁、自动重试、月全量闸门照旧）。断点续传对前端透明——用户失败后“同样条件再点一次”即自动续传。

### 4.2 与现有机制的关系（不冲突，需回归确认）
- **幂等落库**：单条 `approvalRepository.save` 按 proc_inst_id 覆盖 → 续传时未完成员工的“已拉部分实例”会被重拉覆盖，**结果正确、无重复**（与用户期望一致：想跳过的是“已完成员工”，不是单条）。
- **整段 stale 清理**：`deleteStaleMonthDataForSelectedEmployees` 只在**成功整轮后**执行 → 续传的中间态不触发误删。**撤销单兜底不变**。
- **月全量闸门**（每月一次 Redis 锁）：markFullSynced 仅成功打标 → 失败续传不受影响，可重跑。
- **自动重试** `fetchMonthDataWithAutoRetry`：已是 3 次内部尝试；组件续传再叠加一层“下次手动点也能续”，体验更好。
- **ProgressTracker**：进度条照旧（用 Redis `:inserted` 等），断点组件用独立 key，二者不互相覆盖。

---

## 五、考勤暂不迁移（本次不动，最保险）

考勤现有续传已跑生产。抽公用后**并不强制立刻重写考勤**——公用组件的价值在“新代码从第一天就用、降低未来复制粘贴”，考勤可留待日后有余力再平滑迁移（迁移时用组件多 slot 支持其 7 步多员工集合）。**本次改考勤 = 零改动、零回归风险。**

---

## 六、边界与风险

| 项 | 风险/对策 |
|---|---|
| fingerprint 必须足够精确 | 若两次员工集合/日期/类型不完全一致→按“从头”处理，避免续到错任务。指纹由调用方负责拼稳。 |
| Redis 清空/重启 | Redis 数据无 → `begin` 判无断点 → 从头，安全。 |
| 单员工内部分完成 | 接受“整员工重拉”（未完成员工全重拉）；不做“单条续传”（复杂度收益比差，用户已认可按员工跳）。 |
| 并发 | 任务锁（审批 tryBeginFetch / 考勤 tryBegin）已按公司互斥，组件无需再自旋锁。 |
| SUCCESS 后手动再点 | 默认“从头”（=刷新语义），满足“用户主动重拉更新数据”。 |
| 撤销单清理完整性 | 不因续传而变差：整段清理仅在成功整轮执行；续传只影响“已完成员工是否重拉”，不影响成功后的清理动作。 |

---

## 七、回归/验收清单（改完自测用）

1. **正常全量**：首次跑通，返回成功，断点 status=SUCCESS，无残留阻塞。
2. **中途失败续传**：造一个员工在中段抛异常 → 任务 FAILED → 同条件再点 → 日志出现“从断点恢复，跳过已处理 N 员工” → 只处理剩余员工 → 全部成功 → 数据完整、无重复。
3. **换参不续传**：失败后改日期/改员工 → 再点 → 从头（fingerprint 不一致）。
4. **SUCCESS 后再点**：成功后再点 → 从头全拉（刷新语义）。
5. **与月全量闸门**：本月未成功全量时，失败续传可继续；成功后本月再点全量被拦（闸门正常）。
6. **撤销单**：续传跑完后，窗口内被钉钉撤销的单仍被清理（成功整轮触发）。
7. **Redis 清空**：清 Redis 后点 → 从头，不报错。

---

## 八、关联代码位置（备查）
- 审批主循环：`HrmAttendanceApprovalSyncServiceImpl.doFetchMonthData`（约 260–463 行）
- 审批幂等落库：同文件 `approvalRepository.save(entity)`（约 442 行）
- 审批失败标进度：`HrmAttendanceApprovalServiceImpl.fetchMonthData` catch（约 411–416 行）
- 审批自动重试：同文件 `fetchMonthDataWithAutoRetry`（约 607 行）
- 考勤续传参照：`HrmAttendanceDataServiceImpl.SyncData / SyncDataWithResume`（约 631 / 660 行起）
- 通用底座：`common/ProgressTracker`、`common/Redis`、`common/MonthlyFullSyncGuard`(先例)

---

## 九、落地实现记录（2026-09-09 已编译通过）

### 9.1 关键设计决策（用户确认，重要）
- **「继续」= 做法A 原地续传**：通知中心点「继续」→ 后端读断点参数 → 用同参原地重发，不跳页。
- **「继续」出现时机 = 任务真正停且终态失败**：对齐考勤语义——自动重试期间**不得**显示「继续」，仅在自动重试全部耗尽、任务彻底结束后置 FAILED 才可续。**不做「重连成功后重试」显式预探测**（现有指数退避已隐含等连接恢复，预探测不可靠收益低）。
- **通用抽取范围**：本次只做「审批侧正确性小修正」，不动考勤（考勤无此 bug 且跑生产）；暂不做「抽通用自动重试编排 + 迁考勤」的大重构（后续另排）。

### 9.2 审批「过早 FAILED」修正（逐尝试失败 → 仅终态失败才 FAILED）
- 根因：审批在 `doFetchMonthData` finally 对**每次尝试失败**都置断点 FAILED → 自动重试 backoff 间隙断点=FAILED → 「继续」会过早出现。考勤用 `markSyncRetrying` 重试期保持 RUNNING、仅终态 FAILED，故无此问题。
- 修正：
  1. `common/ResumableJobCheckpoint` 新增 `markTerminalFailed(biz, companyId)`（按公司置 FAILED，仅当 fingerprint 存在=确开跑过才置，避免无意义可继续断点）；`fail(ctx)` 委托之。
  2. `HrmAttendanceApprovalSyncServiceImpl` 两处 fetchMonthData finally 的 `failActiveResumeIfNeeded()` → `clearActiveResumeCtxIfNeeded()`（只清内存 ctx，不置 FAILED）。逐尝试失败断点保持 RUNNING，auto-retry 下次 `begin()` 续传。
  3. `HrmAttendanceApprovalServiceImpl.fetchMonthDataWithAutoRetry`：终态 throw（attempt>=max 或 请选择/重复员工）前注入 `resumableJobCheckpoint.markTerminalFailed(BIZ_APPROVAL, getCompanyId())`。
- 附带认知：ProgressTracker(前端进度条)早已对齐（重试期置 RUNNING）；"过早 FAILED"只存在于断点组件 status。

### 9.3 后端「继续」接口
- 审批 `HrmAttendanceApprovalController`：
  - 抽出公共 `submitApprovalFetch(BO,companyId,info)`（互斥 tryBeginFetch + submit + finally finish）。
  - `fetchMonthData` 复用（保留月全量闸门 4001）。
  - 新增 `POST /continueFetch`：`hasResumable`(防 SUCCESS 后绕过月门) → `getSavedPayload` 读 canonical → `parseApprovalPayload`(month|start|end|types|emps，emps=ALL→空列表) 重建 BO → `submitApprovalFetch`（不再做月全量闸门：能 FAILED=未成功全量）。无可续/已成功 → code 4002。
- 考勤 `HrmAttendanceDataController`：新增 `POST /continueSync`：`getSyncProgress()` 判 status=FAILED + params 非空(EmpIDs|begin|end) → split 重建 EmpID/Begin/End → 互斥 tryBegin + markSyncQueued + submit SyncDataWithAutoRetry。无可续 → code 4002。未判定"是否原全量"(按 partial 提交；失败全量续传成功不重打月门，可接受边角=允许多一次全量，安全方向)。

### 9.4 失败通知 content 规范化（任务#20，前端识别用）
- 考勤 `HrmAttendanceDataServiceImpl.saveFailedSyncProgress`：新增把运行中通知(type201, key `attendance:notification:sync:{companyId}`) content 置 `同步失败` + del mark（对齐审批"获取失败"；修复失败后通知停"进度X%"被误判仍运行一直轮询的旧 gap）。
- 前端据此识别可续传失败行：审批 type203+content=获取失败；考勤 type201+content=同步失败。

### 9.5 前端「继续」按钮
- `NotificationCenter.vue`（右上弹窗）+ `NotificationList.vue`（/hrm/notification 查看全部页）：
  - `isResumableFailure(row)`：type203+获取失败 或 type201+同步失败。
  - `handleContinue(row)`：type201→`continueAttendanceSync()`；type203→`continueAttendanceApprovalFetch()`。code4002→warning"暂无任务可继续"；code202/alreadyRunning→info"任务进行中"；否则→success+`fetchNotifications()`（新的"进度 X%"运行通知自动被现有轮询展示）。
- API：`approval.js` 加 `continueAttendanceApprovalFetch`；`employee.js` 加 `continueAttendanceSync`。

### 9.6 验收补充
1. 自动重试期间通知中心**不**出现「继续」（断点 RUNNING，非 FAILED）。
2. 3 次全失败后任务停止 → 断点 FAILED → 该失败通知出现「继续」。
3. 点「继续」→ 后台从断点续传（跳过已完成员工），进度条恢复走动；再失败仍可续。
4. 换参后的新任务失败、与上次 SUCCESS 无关等情形不误显「继续」。
5. 考勤失败通知不再无限轮询（终态"同步失败"）。
