# Requirements

## Context
- 集团 SaaS：4 家公司起步、后续增至 ≤8 家；每公司 ≤500 员工，全集团峰值 ≤4000 人。
- 三端：PC 管理端（hr_web）+ 微信小程序员工端（排班小程序，/mp/*）+ 本后端（hainan 多租户）。
- 数据边界：各分公司数据必须落各自租户库 `hr_XXXX`，跨租户数据（公司列表、小程序绑定、接口权限映射）放系统库 `hrsystem`。
- 用户侧交付口径：错误文案全中文、失败可定位；异步长任务必须有真实进度与完成确认；员工对账/匹配严禁只按姓名。

## 全局非功能需求
- 安全：JWT 裸 token 请求头 + 拦截器三分支鉴权（见 development.md）；改密后旧会话全失效；跨公司加密导出（AES-256 ZIP + 密码响应头）；敏感值不入源码；Tomcat 请求头上限 ≥64KB；跨域按 Origin 回显、预检放行。
- 性能与稳定（2026-08-30 全库审查结论，规则细则见 requirements 备份 2026-08-30 小节）：
  - 钉钉同步链路快速失败禁死循环；事务只包落库段；单例禁共享可变状态（users 字段、SimpleDateFormat→ThreadLocal）。
  - 同步/审批获取/排班提交为"异步任务 + 按公司互斥锁 + 进度键按公司隔离"，每月 1 号 ≤8 家并发互不干扰、不排队等待；失败自动重试（默认 2 次指数退避，考勤断点续传、审批幂等重拉）。
  - 薪资核算先取锁再开事务；社保生成按公司 tryLock；动态调度、Redis TTL、附件/FTP/导出资源释放、日志表保留期清理均有明确规范。
  - 批量钉钉接口优先"日期段 + 员工批次"，禁止退化为单员工循环放大调用量。
- 备份与留存：数据库备份服务 + `restore-snapshot_*` 24h 清理；钉钉调用日志 `postresultlog` 90 天 / `ddtaskresult` 7 天自动清理。
- 兼容与交付：不改既有接口路径；DDL 先行（docs/sql 幂等脚本覆盖 5 租户库）；Excel 下载必须带 `Content-Disposition` 与 `fileDownload=true`；导入接口失败必须返回错误不得假成功。

## 功能需求索引（每功能一行，细则见对应模块文档）
| 功能 | 需求要点 | 模块文档 |
|---|---|---|
| 登录/账号/角色权限/菜单/备份/看板 | 两级菜单权限、系统库账号索引、集团总表行政经理专属 | modules/system.md |
| 部门/花名册/合同/薪资字段 | 唯一性校验、表头驱动导入、无固定期限合同、四个薪资动态字段、加密部门明细 | modules/employee.md |
| 排班/单双休 | 产品-岗位-人员、自定义班次、tbplanlist 事实表、上传电话规则、周/日历双粒度单双休 | modules/attendance-scheduling.md |
| 考勤同步/审批数据 | 7 步 sync 链路、审批 COMPLETED+agree、集中同步按公司隔离、自动重试 | modules/attendance-sync.md |
| 加班/夜班统计/考勤汇总 | 资格=生产+固定月休、应出勤班制按 rest_type、实际/应计出勤公式、手工保存 | modules/overtime.md |
| 薪资 | 计薪口径、应计出勤、病假/个税/工会费规则、五项基本工资配置、月记录恢复 | modules/salary.md |
| 奖金 | 发放/只计税双表双菜单、只进个税累计收入 | modules/bonus.md |
| 社保 | type=12 医疗长期护理 + is_enabled 合计、一键设置保险金额、停保名单 | modules/insurance.md |
| 数据字典/运行配置 | sn/name 自定义、日志保留期、跨域、profile/打包 | modules/dataconfig.md |
| 小程序 /mp/* | 契约见 hr_miniapp/docs/development.md；tbplanlist 与 PC 统一 | modules/miniapp.md |

## Open Questions / TODO
- 钉钉 `qyapi_aflow` 审批读取权限需管理员为当前应用开通，否则线上审批抓取仍失败。
- 性能遗留（2026-08-30 审查未修批次）：加班/夜班"开始统计"逐员工 SQL 风暴；员工导出动态字段 N+1；跨公司导出 XSSF DOM 堆内聚合；薪资导出 N+1 与多副本；审批抓取串行无限流（限流器仅 autoTask 生效）；Excel 批量导入 POI/hutool DOM 按要求保持不动。
- 排班提交任务进度为应用内存态，服务重启后未完成任务轨迹丢失；是否补任务表待评估。
- 加班/夜班更细口径（免计时长、休息时段扣减、特殊班次例外）待业务补充。
- 文档空缺：排班申请审批（workPlanApplication）、招聘渠道（/hrmRecruitChannel）、临时工（/tempworker）、租户开通（TenantProvision）、报表（/report）暂无轮次记录，改动前先扫代码。
- 月度定时任务（autoTask，当前全 profile 关闭）若启用，须接入与手动同步相同的按公司互斥机制。
