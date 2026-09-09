# 系统管理（登录 / 操作员账号 / 角色权限 / 菜单 / 备份 / 看板权限）

菜单前缀：`/hrm/system/*`（登录用户管理、角色权限分配、菜单权限清单）+ `/manage/*`（右上角齿轮系统设置）+ `/tbLoginUser`、`/tbMenu`、`/tbRoleMenu`、`/hrsystem/login|logout|changePassword`。

## 需求要点
- 权限粒度为"模块 + 子菜单/顶级页面"两级（`tbmenu.pid=0` 为模块），不含按钮级与数据范围权限；角色必须至少配置一个启用页面权限，保存时后端自动补父模块。
- 每个可登录账号必须绑定启用角色且角色已配置权限；登录成功返回 `token + rolemenu + menuTree`，前端据此渲染菜单与路由守卫。
- 创建/编辑登录用户必须同时维护：租户库 `tbloginuser` 主数据 + 系统库 `hrsystem.tbAllUserList` 账号索引（`${hrm.system.database}.tbAllUserList`）；禁止跨公司同账号抢占；保存索引前先删同公司同账号旧索引。
- 新建登录用户缺省部门取 `hrm_dept.parent_id=0` 最顶级部门；删除用户需同时清租户用户与系统库账号索引（`POST /tbLoginUser/Delete/{id}`）。
- 右上角齿轮系统设置纳入同一权限体系：需具备 `/manage/insurance-scheme|vacation|attendance|salary` 任一子菜单权限。
- 接口不能只依赖前端隐藏菜单：`tb_api_permission`（系统库，按接口前缀映射菜单路由）+ `ApiPermissionPathSupport` 做访问校验，复用接口支持"任一关联菜单授权即可访问"。
- 数据看板"集团总表"仅行政经理可用：接口 `/hrmEmployee/departmentDetailGroupSummary` 内部校验行政经理角色与 `groupSummary:*` 看板权限，无权者 403"无权访问集团数据"。

## 设计与契约
- 登录链路：`LoginController#Login` → `hrsystem.tbAllUserList` 按 account 查 CompanyID（DISTINCT 去重，同公司重复行可登录，跨公司返回中文配置错误）→ `hr_${companyId}.view_loginuser`（tbloginuser×hrm_dept×tbroletypes 内连接，任一关联缺失表现为"账号不存在"）→ 签发 token。
- **切换企业（登录态内）**：`POST /hrsystem/switchCompany{companyId}`（`LoginController#switchCompany`）。多租户路由以 token 内嵌 companyId 为唯一来源，切换=换发绑目标企业的新 JWT+重载该企业 menuTree。校验：CompanyContext(当前 token) 取 account → `getCompaniesByUserName` 实时白名单核对 target ∈ 候选（否则拒绝"不可访问"）→ `loadTenantUser`（存在性+canLogin）→ 组装新 LoginUserInfo（沿用当前 sessionSeed，**不 bump**，原企业 token 保留至自然过期以支持切回/多标签）→ `fillPermissionMenus`（顺带触发目标租户池建池预热）→ 签发 token。目标库不可达返回 500 中文提示并**保持当前登录态**。`POST /hrsystem/switchCompany/candidates` 返回 `{currentCompanyId, companies[{companyId,companyName}]}`。两端点不在 skipUrls、不在 api_permission 映射表 → CompanyInterceptor 默认分支仅操作员 token(account 非空)可访问。无 DDL。
- 退出登录：`POST /hrsystem/logout?token=...`，jti 写 Redis 黑名单；改密成功 `bumpSessionSeed` 使在途会话全部失效。
- 权限辅助类：`MenuPermissionSupport`（菜单归一化/补父级/空权限校验/构建 menuTree）、`ApiPermissionPathSupport#resolveRequiredMenuPaths`（多菜单路径映射）；`CompanyInterceptor` 在 token 解析后校验接口菜单权限。
- 关键表：`tbloginuser`（id 自增）、`tbroletypes`（`canUse` 驼峰列）、`tbrolemenu`（`role_menu_id` 为 bigint 雪花主键，JPA 实体与 MyBatis-Plus 实体均须 Long/显式主键）、`tbmenu`（历史 22 条 path 为 NULL，迁移只补 path 不重建）。
- 数据库备份：`DatabaseBackupService`；`cleanupExpired` 同时清理 `restore-snapshot_*` 目录（保留 24 小时）。
- 菜单种子/补权脚本见 `docs/sql/2026-06-16_menu_permission_seed.sql`、`2026-06-17_grant_system_settings_admin.sql`、`2026-06-18_grant_internal_page_permissions_hr_0001_to_hr_0005.sql`。
- 企业权限保存（CompanyPermissionController#save）：全量替换语义，任一公司失败 `setRollbackOnly()` 整体回滚（catch 内返回不触发回滚的坑已踩）；同步菜单映射前先经 `insertMenuIfMissing` 从基准库补齐目标库缺失的 `tbmenu` 定义，防止登录报"未配置菜单权限"；`insertRoleMenu` 必须显式雪花 `roleMenuId`（`tbrolemenu` 非自增主键）。
- **生产库基线**（153.0.237.98:3306，内网 192.168.0.26 同实例）：`hrsystem` 多租户结构齐全，**仅缺 `tb_backup_record`、`tb_api_permission` 两表**——缺后者时接口权限回退内置默认（可运行但功能退化），部署需建表并灌种子（`docs/sql/2026-06-16_menu_permission_seed.sql`）。租户库 hr_0001~0005 表结构完整无需补。（来源：`../archive/memory/2026-08-28.md` 生产基线盘点）
- 前端算法知识库（无后端接口）：hr_web 顶栏"算法知识库"弹窗展示本项目全部业务算法公式，数据文件 `hr_web/src/constants/knowledgeBase.js`（条目含公式与 `文件:行号` 代码位置）；**后端算法改动时必须同步该数据文件**，设计/契约见 `hr_web/docs/modules/system.md`。

## 近期变更
- 2026-09-09 权限管理模块归拢 + 菜单命名统一：`tbmenu(1000)`「系统管理」改名「权限管理」（此前前端 `router/config.js` 里 `/hrm/system` 分组叫「权限管理」，DB 叫「系统管理」，两处不一致）；「看板权限管理」统一为「看板权限」；散落在顶级的权限项 `1030 数据统计`、`5000 排班小程序`（含子 `5020 排班数据加载`）挂到 1000 下。归拢后出现三级结构，**原后端只支持两层**，已同步改递归：`TbMenuService#queryMenuList`（权限树展示）、`MenuPermissionSupport#buildAuthorizedMenuTree`（登录菜单树，三级节点丢失会导致按 path 鉴权失败）、`CompanyInterceptor#hasMenuPermission` 与 `DashboardServiceImpl#requireDashboardPermissionAdmin`（按 path 匹配）。数据迁移：给已勾 1030/5000/5020 但缺 1000 的角色补授权（否则整支菜单从用户菜单树消失）；`seed-data.sql` 同步菜单层级/命名并给角色 1 补 1000、5000（原本只有 5020 无父链）。前端 `utils/permission.js`：`filterRoutesByPermission` 剔除「父路由无任何可访问子页面」的分组，避免勾了父菜单却没勾子菜单时出现空白分组入口。迁移脚本 `docs/sql/2026-09-09_menu_permission_grouping.sql`（{DB} 占位，按租户库执行）。测试 17 项全通过（`TbMenuServiceTest` / `MenuPermissionSupportTest` / `ApiPermissionPathSupportTest`）。另：`1030`→「小程序数据统计」、`5020`→「小程序排班数据加载」（`5000` 保持「排班小程序」）——这两条只映射 `/mp/*` 小程序接口（`tb_api_permission`：18 条 `/mp/dashboard/*` → 1030，3 条 `/mp/mySchedule|/mp/schedule/query` → 5020），**PC 端没有也不该有入口**；原名与侧边栏第一个入口「数据统计」（`/hrm/blank`，即 `views/hrm/home/Blank.vue` 看板页，对所有账号默认开放）重名，导致误以为勾了它才该出现该入口。侧边栏由前端 `router/config.js` 渲染（`SideMenu.vue` 读路由 `children`/`meta.name`），DB 菜单树只提供 path 集合供后端鉴权，两者互不相通。
- 2026-09-09 修复租户开通失败（报错 `Field 'id' doesn't have a default value` @ `TenantProvisionService.provision:141` / `insertDdAccount`）：根因 `hrsystem.ddAccount.id` 为 `int NOT NULL PRIMARY KEY` 但历史建表未设 AUTO_INCREMENT 也无默认值，与 `tbcompanylist.id`（自增）不一致；`insertDdAccount` 的 INSERT 未提供 id 被 MySQL 拒绝。修复：`insertDdAccount` 显式提供 `id = MAX(id)+1`（全环境通用，不依赖表结构是否自增）；本地库同步 `ALTER ... MODIFY id int NOT NULL AUTO_INCREMENT` 对齐系统表约定；`hr_web` 仅调 `/tenant/provision` 无需改动。验收：保存成功。
- 2026-09-09 补齐「数据统计」菜单（`/hrm/dataStatistics`，id=1030，pid=0，`showMenu='0'` 仅作权限开关）：此前 `ApiPermissionPathSupport` 把 15 个小程序统计接口 `/mp/dashboard/*` 映射到该菜单，但 `tbmenu` 里根本没有这条菜单 → PC「角色权限分配」无处可勾、小程序统计接口必然被拦。已写入 `seed-data.sql`（菜单行 + 系统管理员角色 2 授权），本地 hr_0001~hr_0008 已补菜单并授权给管理员/系统管理员/人事专员，本地 `hrsystem.tb_api_permission` 已导入 15 条映射。**设置入口**：谁能看 → 权限管理→角色权限分配；看哪些卡片 → 权限管理→看板权限。同步修正 `docs/sql/2026-09-09_miniapp_dashboard_permission.sql`（旧版误写 `hrsystem.tbmenu`，实际 `tbmenu`/`tbrolemenu` 在租户库 `hr_XXXX`，仅 `tb_api_permission` 在系统库）。

## 历史摘要
- 2026-08-31 新租户开通缺表缺菜单修复 + 开通租户权限误拦修复（详见 `archive/changelog.md`）；
- 2026-06-16：权限管理与登录用户创建上线（MenuPermissionSupport/ApiPermissionPathSupport、登录加载菜单写 token、登录用户保存校验部门/角色/权限、系统库账号索引独立事务）；菜单种子 SQL 补齐 path 与系统管理菜单。
- 2026-06-17：系统设置齿轮权限可见性修复（父级菜单不能单独授权、`TbMenuService` Integer 引用比较改 `Objects.equals`、hbadmin 补权 SQL）；假期扣减 `deduction_id` UUID 主键改 String；Tomcat `server.max-http-header-size=65536`（token 约 8KB）；登录用户默认部门与删除接口上线。
- 更早轮次与细节见 `../archive/changelog.md` 与 `../archive/development-2026-08-30.bak.md`。
