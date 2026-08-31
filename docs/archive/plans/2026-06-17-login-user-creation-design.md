# 登录用户创建保存设计（2026-06-17）

## 背景

创建登录用户保存报错的根因不是单一字段问题，而是创建链路没有完全按登录链路涉及的库表来设计。登录时先从系统库账号索引表定位租户，再从租户库登录视图读取用户、部门、角色和权限；因此创建登录用户必须同步维护这些依赖。

## 登录链路涉及库表

1. 系统库 `${hrm.system.database}.tbAllUserList`：按 `account` 查询 `CompanyID`，用于确定用户所属租户。
2. 租户库 `hr_${companyId}${hrm.system.databasesuffix}.view_LoginUser`：按 `Account` 查询登录用户信息。
3. `view_LoginUser` 底层依赖租户库 `tbloginuser`、`hrm_dept`、`tbroletypes`，任一内连接缺失都会导致登录表现为账号不存在。
4. 角色权限链路依赖租户库 `tbrolemenu`、`tbmenu`，登录后用于生成 `rolemenu` 与 `menuTree`。

## 创建与编辑规则

- 新建登录用户时，先校验账号、密码、部门、角色与角色权限，再保存租户库 `tbloginuser`，最后写入配置系统库 `${hrm.system.database}.tbAllUserList`。
- 部门必须在 `hrm_dept` 中存在；角色必须在 `tbroletypes` 中存在且启用；角色必须至少具备可访问子菜单权限。
- `tbloginuser.id` 使用数据库自增主键，避免 MyBatis-Plus 生成雪花 ID 与历史表结构不一致。
- 系统库账号索引不允许跨公司抢占：若同一账号已映射到其他 `CompanyID`，创建或编辑时返回中文业务错误。
- 编辑账号时，如账号发生变化，删除当前公司旧账号索引并写入新账号索引，避免旧账号继续定位到该租户。
- 系统库账号索引的查询、保存和删除必须临时清空 `CompanyContext`，并使用独立 `REQUIRES_NEW` 事务执行；否则外层租户事务会让动态数据源继续复用租户连接，导致总库 SQL 在租户库连接上执行而保存报错。

## 验收口径

- temp 环境应写入 `hrsystem_dev.tbAllUserList`，而不是硬编码 `hrsystem.tbAllUserList`。
- 创建成功后，登录接口能通过系统库索引定位租户，并通过租户库 `view_LoginUser` 查询到用户。
- 无效部门、停用角色、空角色权限、跨公司重复账号都应在保存阶段返回明确中文错误。
