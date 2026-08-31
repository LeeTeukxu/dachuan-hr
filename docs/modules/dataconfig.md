# 数据与运行配置（数据字典 / 报表 / 临时工 / 租户开通 / 跨域 / 部署与构建）

菜单前缀：`/dict`、`/tbCompanyList`、`/report`、`/tempworker`、`/tenant`。（临时工、租户开通 TenantProvision、报表暂无文档轮次，见 TODO。）

## 需求要点
- 数据字典可自定义：`/hrm/dataConfig/index` 不再限定"工段/车间"，用户直接输入 `类型(sn)` 与 `值(name)`；`dtid` 退化为内部树元数据（新增补默认值、编辑合并防洗掉）；`getbyDtId/getbyPId` 把 `sn` 回填 `TreeNode.type` 供前端展示；添加排班页按类型文本语义分流"工段/生产车间"下拉，兼容历史错桶数据。
- 钉钉日志表保留期：`DingTalkLogRetentionCleanupTask` 每天 5 点按公司清理 `postresultlog`（默认 90 天）与 `ddtaskresult`（processed=200，默认 7 天）；2000 条/批独立事务、每表每公司单轮 ≤50 批；配置 `hrm.dingtalk.log-cleanup.*`；独立 daemon 调度不受 `hrm.scheduling.enabled=false` 影响。
- 数据库备份：`DatabaseBackupService.cleanupExpired` 额外清理 `restore-snapshot_*` 目录（保留 24 小时）。
- 动态定时调度（`ScheduledController`）：注册表 `ConcurrentHashMap`；同编号重复启动先 cancel 旧 future；调度池 poolSize=4 + 命名前缀。
- Redis 长期键必须有 TTL：排班自定义班次补充元数据 30 天、`ClassList_*` 30 分钟（缺失走本地快照兜底）；`RedisImpl.mSet/mSetNx` 禁止遍历中结构性修改。
- 跨域：`CrossDomainFilter` 按 `Origin` 回显允许源、放行 `token/Content-Type/Authorization`、预检 OPTIONS 直接 200；`CompanyInterceptor` 在 token 校验前放行 OPTIONS；禁用局部 `@CrossOrigin` 叠加；下载需暴露 `Content-Disposition, X-Archive-Password`。
- 部署/构建：`application.properties` 默认 profile 必须 `dev`（`ApplicationProfileConfigTest` 守卫）；生产打包走 `target/package-prod.sh` + `package-prod.command`（临时副本改 prod，校验 jar 内 profile）；三个 `application-*.properties` 保持 UTF-8 无乱码注释。

## 设计与契约
- 多租户数据源：租户连接元数据存系统库 `hrsystem.tbCompanyList.url`（含 `User Id/Password`）——主库改密必须同步更新该表；`ConnectionParsor#buildMysqlJdbcUrl` 统一拼接含 `allowPublicKeyRetrieval=true` 的 URL；`DataSourcePoolConfigurator` 统一连接池参数（每租户 maximumPoolSize=8、minimum-idle=0）。
- 附件/FTP 资源纪律：下载 finally 归还连接、附件下载后删 Temp、上传 `transferTo` 流式落盘、zip4j try-with-resources、`ExcelWriter.finish()` 收 finally。
- 进度状态类可变字段加 `volatile`；审批 `FETCH_PROGRESS_MAP` 终态 2h/运行态 24h 过期清理。
- 系统库职责边界：`hrsystem` 只放跨租户数据（tbCompanyList、miniapp_user_binding、tb_api_permission 等），租户业务表不放系统库。

## 近期变更
- 2026-08-30 性能审查第二批（本模块部分）：动态调度修复、`AttendanceDbLock` 改按 companyId 锁、Redis TTL 补齐与 mSet 修复、附件/FTP/导出资源泄漏收口、日志保留期清理任务上线、prod mapper DEBUG 降 INFO、备份快照目录清理。
- 2026-08-24 应用环境配置文件乱码修复：清理 `server.servlet.encoding.*` 注释乱码、统一小程序 appid 占位文本，UTF-8 编码不变。
- 2026-08-22 数据字典类型和值自定义上线（DictDataController `AddType` 可选 + sn/name 直存 tbdictdata）。

## 历史摘要
- 2026-08-18：服务器部署跨域修复（CrossDomainFilter bean 化 + OPTIONS 放行 + 线上核验）；prod 一键打包脚本；Maven 默认 profile 回归守卫。
- 2026-06-13：`mvn clean package` BOOT-INF/lib 反斜杠路径修复（跨平台 `/` 写法）。
- 2026-05-13：动态数据源 `allowPublicKeyRetrieval` 缺参修复与 `tbCompanyList` 租户密码同步更新；2026-04-03：连接池统一配置与同步链路连接异常重试。
- 2026-04-09：dev 日志落文件（`logs/hainan-dev.log`）。细节见 `../archive/changelog.md`。
