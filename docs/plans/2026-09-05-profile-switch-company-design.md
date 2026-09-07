# 方案设计：个人信息界面「切换企业」（可登录多企业账号，登录态内切换租户）

- 日期：2026-09-05
- 状态：**已实现 + 验收通过**（代码级验收 + 真机 UI 回归验收均完成，见 §11）。
- 涉及项目：`hainan/`（后端）、`hr_web/`（PC 前端）
- 需求编号：#…（新主题；设计/契约已并入两端 `modules/system.md` 并登记 development.md 索引）

## 一、背景与现状（已核实的机制，非推测）

多租户整链路是 **「JWT 内嵌 companyId → 路由到对应租户数据源」**：

1. `LoginUserInfo` 全字段（含 `companyId/account/roleId/companyName/menuTree/sessionSeed`…）被序列化进 JWT 的 `Content` claim（`JWTTokenUtils.java:41`）。
2. 每次请求：`CompanyInterceptor.preHandle` 解析 token → 校验吊销/封禁/会话种子 → `CompanyContext.set(LoginUserInfo)`（`CompanyInterceptor.java:79-113`）。
3. 取连接时 `DynamicDataSource.determineCurrentLookupKey()` 读 `CompanyContext.get().getCompanyId()` 路由（`DynamicDataSource.java:26-30`）。
4. 连接池：`CompanyDataSourceProvider` 静态缓存 + 懒建，max=8/minIdle=0/idleTimeout=180s，空闲 30 分钟驱逐、上限 500（`dataconfig.md`、`CompanyDataSourceProvider.java:41-43`）。命中缓存秒切；**首次冷建池约数百 ms~1s**；目标库 url 建连失败会再走一次"主库同源回退"，双重失败才报错（`CompanyDataSourceProvider.java:154-178`）。
5. 同一账号可属多企业：系统库 `hrsystem.tbAllUserList`(LEFT JOIN tbcompanylist) 出候选企业列表（`getCompaniesByUserName`，SQL 在 `LoginUserMapper.java:21`）。
6. 登录已支持多企业选择：`/login` 不带 companyId 且候选>1 → 返回一次性 `confirmToken`(Redis `hr:confirm:*`, TTL 300s) + 企业列表；`/confirmCompany` 用该 token + companyId 生成绑该企业的新 JWT（`LoginController.java:124-127,175-230`）。**密码在首次 /login 已验，/confirmCompany 不再验密**，且切换企业**不会** bumpSessionSeed（种子按 account，不按企业）。
7. 前端登录态：Vuex(state.user_info) + localStorage(`token`/`user_info`)。路由全量静态注册、靠 `localStorage.user_info.menuTree` 按路由守卫过滤 + 侧栏组件过滤（`router/router.js:43-47`、`store/index.js:3-8`）。业务接口基本不显式带 companyId，**纯靠后端 token 路由**。
8. 「个人信息」页在 `hr_web/src/views/hrm/profile/Index.vue`，含基本信息卡（显示所属公司 companyName）与安全设置；入口在 `TopHeader.vue` 头像下拉。改密码/退出已有实现。

## 二、需求与本次目标

需求原始表述：
1. 当前登录账号加载对应**可访问企业**列表。
2. 切换企业后**整站数据**切到该企业（各租户业务数据、菜单/权限、顶部企业名等），且不出差错。
3. 动态切换数据池连接要**注意性能、别崩**。
4. 切换后**不影响整个系统运行及流畅度**（切换本身即整站刷新语境，重点是切换中不卡死、不串数据、不拖累他用户/后台任务）。
5. 先出方案讨论，改完把设计与契约写进两端模块文档并更新索引。

> 说明：本方案讨论稿不含"实现代码/数据库改动"，仅界定技术路线、接口契约、前端刷新策略与风险对策，供评审。

## 三、核心判断

**由于「数据路由 = token.companyId」是全站唯一事实来源，且菜单树按企业加载、前端 localStorage 缓存了整份 user_info/menuTree，"切换企业"在本质上 = "用同一账号换发一份绑定目标企业的 JWT + 重载该企业 menuTree + 整站刷新"。**

因此不需要引入「运行时并发切换数据源」这种高风险做法（避免切一半的旧请求落在旧库、新请求落新库而串数据，也正是需求 4 的坑）。

## 四、方案总览

```
[个人信息页/头像下拉] 企业切换入口
        │  点击「切换企业」
        ▼
GET 候选企业(带当前token)      ← 也可复用登录返回的 companies，但为保"可访问且有效"实时性走接口
        ▼
弹企业选择(类登录弹窗, 排除当前企业/允许)  → 用户选目标企业
        ▼
POST /hrsystem/switchCompany { companyId, currentToken? }
        ▼  后端：校验 token→account 仍在目标企业候选列表 → 载目标租户用户+该企业 menuTree → 发新 JWT
        ▼  返回：新 token + 新 LoginUserInfo(companyId/companyName/menuTree 全量)
        ▼  前端：用 login 同款 handleHrLoginResult 写 localStorage+Vuex → 强刷整页(带目标企业)
        ▼
路由守卫按新 menuTree 放行 → 全站请求已带新 token → 落到目标企业库
```

### 关键设计决定（讨论点）

- **D1 新端点 vs 复用 confirmCompany**：新增专用 `POST /hrsystem/switchCompany`（走已登录 token 鉴权），不改既有 confirmCompany 一次性语义。理由：confirmCompany 依赖一次性 confirmToken 且只在登录弹窗链路可拿；切换场景是**登录态内**，应直接以「当前有效 token 即证明账号合法」为前提换发新企业 token。
  - 后端在 switchCompany 内：`CompanyContext`(当前 token 的 LoginUserInfo) → 取 account → `getCompaniesByUserName` → 校验目标 companyId ∈ 候选 → `loadTenantUser(account, cid)`（顺带查 canLogin / 存在性）→ 组装新 LoginUserInfo(account/companyId/suffix/companyName/sessionSeed=当前) → `fillPermissionMenus` → `JWTTokenUtils.getToken`。
  - **是否 bumpSessionSeed**：讨论点（见 §7 安全，倾向旧 token 继续短时有效以便"切回"；若业务要求每次切换作废旧企业会话，则对 account bumpSessionSeed——代价是所有企业会话全失效，需注意）。
- **D2 前端刷新策略 = 整页 reload，不做原地热切换**：切换成功后写 localStorage 再 `window.location.reload()`（或跳 `/hrm/blank`）。理由：Vuex/user_info 是全量快照、静态路由靠守卫过滤、多页面各自缓存 user_info 派生数据；原地切易残留旧企业数据（正是需求 2/4 的风险）。reload 后一切以新 token+新 menuTree 重建，简单且无串数据。
- **D3 候选企业加载**：新增轻量接口 `GET/POST /hrsystem/switchCompany/candidates`，后端以当前 token 的 account 实时查 `tbAllUserList⊕tbcompanylist`，仅返回「登录态账号可访问企业」——满足需求 1，且比复用登录快照更不易被"账号后来被剥夺某企业权限"误导。（讨论点：是否直接读 token 里没有的企业列表必须走后端。）
- **D4 性能/不崩对策**：切换本质是「一次冷建池的边界成本」，可接受但需预热与失败优雅降级（见 §6）。
- **D5 连接池预热与驱逐防抖**：切换目标企业成功后即 `CompanyDataSourceProvider.getDataSource(cid)` 预热一次连接（把冷建成本放在用户看得到的"切换 loading"，而不是散到后续 N 个首屏请求）。并对目标企业 `markActive` 一段时间，防 evictor 恰在刚建池 30min 内把它驱逐。

## 五、接口契约（草案）

### 5.1 `POST /hrsystem/switchCompany`（form-urlencoded）
- 入参：`companyId`（必填）；token 走现有 `token` 头/参。
- 鉴权：需已登录（token 有效），员工 token（account 空）拒绝——复用 CompanyInterceptor 对 `/hrsystem/*` 外的路径不拦，但本端点为登录态专用，需在实现时确保能拿到 CompanyContext；若 CompanyInterceptor 不拦 `/hrsystem/switchCompany`（skipUrls 无），默认 account 非空校验即可满足。**注意**：CompanyInterceptor 会把该路径当作"映射表外"按 account 非空放行，符合要求。
- 成功响应：
```json
{ "success": true, "data": { "token": "<新JWT>", "companyId":"0002", "companyName":"田野XXX", "account":"...", "roleName":"...", "menuTree":[...], "rolemenu":[...], "mustChangePassword":false, "sessionSeed":123 } }
```
  data 结构与 `/login` 成功返回完全一致 → 前端可复用一个写入函数。
- 失败：`success:false` + 中文 message；目标企业库连不上时返回与 confirmCompany 一致的 500 + 中文提示（提示"该企业数据库可能不可访问"），**不消费/不清除当前登录态**，用户可继续留在原企业。
- 约束：目标企业必须在候选列表内，否则 403"当前账号不可访问该企业"。

### 5.2 `POST /hrsystem/switchCompany/candidates`
- 入参：无（token 鉴权即可，也可带可选 companyId 返回当前是否可切）。
- 返回：`{ success:true, data: { currentCompanyId, companies:[{companyId, companyName}] } }`（去重、按 tbAllUserList 实际授权，实时）。
- 用途：个人信息页/切换弹窗的候选数据源（需求 1）。

### 5.3 前端（hr_web）
- `api/login/user.js` 增 `switchCompany(params)`、`fetchSwitchCompanies()`。
- 复用 `handleHrLoginResult`（Login.vue:183-201）做凭证写入，保证 token/user_info/强制改密分支一致；在其成功后统一走 `window.location.reload()` 完成整站重建（改一个"是否 reload"标志位，避免登录页双跳）。

## 六、性能与不崩要点（对应需求 3/4）

1. **冷池只在切换当下**：CompanyDataSourceProvider 懒建 + 测试连接，后续请求全命中缓存，单次切换毫秒级；不引入运行时多池并发切换，从根上规避"新旧库混写/串数据"。
2. **预热**：实现中 `fillPermissionMenus`（内部 `CompanyContext.set(目标企业)` → JPA/MyBatis 走多租户连接提供器）已顺带触发目标租户 Hikari 池建池；故不再额外加显式 `getDataSource` 预热，冷池耗时落在切换请求的返回路径上，前端弹"切换中…"态，首屏请求不吃冷池抖动。
3. **失败优雅降级**：目标库建池/连接失败 → 返回失败并**保持当前企业登录态不受影响**，前端提示后可留在原企业；不崩、不误退登录。这一点对齐 confirmCompany 既有的「保留重试、可定位提示」做法。
4. **不拖累他人**：懒建池本身是按 companyId 隔离的 Hikari 池；切换只新增/命中该租户池，不影响其他用户与后台定时任务已开池（markActive/驱逐跳过机制已存在）。不给 CompanyDataSourceProvider 增加"切换即关别人池"逻辑。
5. **异步任务不受干扰**：后台任务自己 set CompanyContext + markActive，与前台用户切换天然隔离（`DingTalkApprovalAutoSyncTask` 模式）；本次不触碰该机制。
6. **避免误驱逐**：新切换目标池处于"刚访问"，evictor 按 lastAccess 驱逐，30min 空闲才驱逐，切换后正常浏览不会触发；预热接口顺带 markActive 防极端空窗。
7. **线程安全**：切换只在单请求线程内建池，CompanyDataSourceProvider 已有 synchronized 双重检查，无并发写 Map 问题（早前已治理）。本次不新增全局可变状态。

## 七、安全与一致（红线核对）

1. 不引入明文密码、不改 JWT 密钥、不改登录锁定/验证码链路；switchCompany 基于"已登录 token"而非明文凭据，避免再次验密/验证码，降低被打断与泄漏面。
2. 候选企业校验不可省：必须用**实时** `getCompaniesByUserName(account)` 白名单校验目标 companyId，防止越权切换到账号本不可访问的企业（需求 1 的语义底线）。
3. 会话种子（sessionSeed）：**已决定** switchCompany 沿用当前 account 的 sessionSeed（不 bump），使切换前 token 在有效期内仍可用于"切回"，且不误伤多标签/后台；**若产品要求"切换即作废旧企业会话"**，需改按 account bumpSessionSeed——副作用是该账号全部企业会话失效，且需重新登录，属较大行为变更。
4. 多标签页：切换后的整页 reload 会带新 token；request.js 已有"迟到旧 token 响应静默忽略 + timeOut 只清当前 token"保护（requset.js:47-69），不会互相误清。实现中 reload 前要保证 localStorage 已先写新凭证（先 set 再 reload），避免竞态。
5. 菜单一致性：新 token 的 menuTree 来自目标企业角色，路由守卫与侧栏据此过滤；跨企业若有当前路由不在新企业权限内，守卫会落到 `getFirstPermittedPath`（router.js:45），不会 404 白屏。

## 八、改动文件清单（已实现）

后端 `hainan/`：
- `controller/LoginController.java`：新增 `switchCompany` + `switchCompanyCandidates` 两个端点；复用 `loadTenantUser`/`listCandidateCompanies`/`fillPermissionMenus`/`fillCompanyName`。
- `CompanyDataSourceProvider`：**无需改动**（fillPermissionMenus 已顺带建目标池；复用既有懒建/驱逐/markActive）。
- 复用不动：`LoginUserMapper`、`TokenRevocationService`、`MenuPermissionSupport`、`JWTTokenUtils`。
- 测试：`LoginControllerTest` 增 switchCompany/candidates 共 4 条用例（切换成功/越权拒/切当前拒/候选列表），另删既有 1 条无关冗余 stub 使整类跑绿。
- DDL：**无**（不加表不加列）。

前端 `hr_web/`：
- `api/login/user.js`：加 `switchCompany`、`fetchSwitchCompanies`。
- `src/utils/authSession.js`：新增 `saveLoginSession`/`clearLoginSession`（登录与切换统一会话写入）。
- `views/hrm/profile/Index.vue`：基本信息卡标题行加「切换企业」按钮 + 选择弹窗（剔除当前企业），成功后写凭证并整页 reload。
- `views/Login.vue`：`handleHrLoginResult` 改走 `saveLoginSession`（单一事实源）。
- `TopHeader.vue`：未改（未加头像下拉快捷项，属可选增强）。

## 九、回归与验证口径

1. 一号多企业账号：个人信息页能看到可访问企业并切换；切后顶部用户名不变、公司名/菜单/首页数据切到目标企业；业务列表（员工/排班/薪资等抽 3~5 个页面）取数均为目标库。
2. 一号一企业账号：不显示切换入口（或入口禁用 + 提示仅一个企业）。
3. 越权用例：手动构造 companyId 不属候选 → 403，且原登录态仍在。
4. 目标企业库停服/url 错误：切换返回 500 中文提示，用户留在原企业不崩不退登录。
5. 冷/热切换耗时记录；连续快速切换多次不卡、不报连接池异常；切换期间发起原企业请求（迟到响应）不污染新企业（token 不同已静默）。
6. 后台定时任务/他用户在同一实例跑着时切换，互不影响。
7. 前端 build 通过。

## 十、待讨论确认点（评审拍板；代码已按"推荐默认值"落地，如需改可回滚）

- [x] **P1** 新增专用 `POST /hrsystem/switchCompany` + `candidates`（已按此实现）。
- [x] **P2** 会话种子**不 bump**，原 token 保留至自然过期（已实现）；若要"切换即作废旧企业会话"需改为按 account bumpSessionSeed。
- [x] **P3** 仅「个人信息」页放切换入口（头像下拉快捷项未加，可后续增强）。
- [x] **P4** 采用独立实时 `candidates` 接口（已实现）。
- [x] **P5** 切换后整页 reload、停驻 profile（profile 位于权限白名单恒可达）。
- [x] **P6** 一期不做"不 reload 原地热切换"富交互，采用整页 reload。

## 十一、收尾（已执行）

- 归属：并入 `hainan/docs/modules/system.md`（登录链路章节补「切换企业」设计与契约 + 近期变更一行），`hainan/docs/development.md` 功能模块索引系统管理行补 `/hrsystem/switchCompany`。
- 前端：`hr_web/docs/modules/system.md` 已同步（个人信息页切换入口 + `utils/authSession.js` + 刷新策略），`hr_web/docs/development.md` 索引已含 profile。
- 验证：后端 `mvn compile` 通过；`LoginControllerTest` 整类 `Tests run: 12, Failures: 0`；前端 `vite build --outDir /tmp` 通过（53s）。
- 代码级验收（2026-09-05）：逐条核对源码与契约一致——越权白名单校验（`LoginController.java:260-264`）、切当前企业拒绝（253-255）、不 bump 会话种子（270）、JDBC 失败 500 中文提示且不清当前登录态（283-289）、candidates 返回 `currentCompanyId+companies`（311-315）、前端切成功写凭证后整页 reload（profile/Index.vue:130-132）。红线核对：未触密码/验证码/JWT 密钥/连接池治理逻辑；两新端点走操作员 token 鉴权（CompanyInterceptor 默认分支），员工/小程序 token 拒绝。
- 按 project-modification 收尾纪律：四份文档行数均远低于上限；本方案稿状态见文首。
- 真机 UI 验收（2026-09-05，业务侧在真实环境执行）：按 §9 回归口径完成并确认通过——一号多企业切换后公司名/菜单/首页及各业务列表取数均落到目标企业库；一号一企业提示无需切换；越权 companyId 拒绝且原登录态保留；目标企业库停服优雅降级不退登录。功能整体验收通过，见文首状态。
