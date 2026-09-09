-- 2026-09-10 公司切换能力（员工级开关，与 B方案「可见公司清单」共存）
-- 背景：
--   B方案已实现「员工可见公司清单」(hrsystem.mp_employee_visible_company) 与
--   /mp/dashboard/visibleCompanies 接口、越权收敛到 MiniAppDashboardController；
--   但 mp_schedule_permission 仍缺「可切换公司」能力字段。
--   按 2a 字面要求：在每个租户库 mp_schedule_permission 加 can_switch_company，
--   作为「切到非本登录公司」的显式开关（与可见公司清单共存，安全更细粒度）：
--     - can_view_statistics = 0：完全禁数据统计
--     - can_view_statistics = 1 且 can_switch_company = 0：只本公司（默认安全）
--     - can_view_statistics = 1 且 can_switch_company = 1：允许切到 mp_employee_visible_company 授权的其它公司
-- 执行范围：每个租户库 hr_XXXX 执行一次（幂等：列已存在则跳过）。
-- 后续步骤：
--   1. 后端 ApiPermissionPathSupport 新增 ABILITY_SWITCH_COMPANY 与对应路径映射
--      （/mp/dashboard/visibleCompanies 与 dashboard/* 切公司 companyId 时校验）
--   2. MiniAppPermissionServiceImpl/MpPermissionSaveBO 新增 canSwitchCompany 读写
--   3. hr_web MiniappPermission.vue 新增「可切换公司」开关列
--   4. miniapp 切公司动作（statistics.vue / deptStatistics.vue）→ 端点仍走
--      ABILITY_STATISTICS 后端 guardCompany/guardGroup 已在 B方案收敛可见集合；
--      新增 guard：员工请求带非本司 companyId 时需 hasAbility(ABILITY_SWITCH_COMPANY)

-- ---------- 1. 字段（幂等：列已存在则跳过） ----------
SET @DB = '{DB}';

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @DB AND table_name = 'mp_schedule_permission'
                 AND column_name = 'can_switch_company');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `{DB}`.`mp_schedule_permission` ADD COLUMN `can_switch_company` tinyint NOT NULL DEFAULT 0 COMMENT ''可切换公司(切到非本司) 0=否 1=是'' AFTER `can_load_schedule`',
  'SELECT ''can_switch_company 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 验证 ----------
SELECT '字段（应含 can_schedule / can_view_statistics / can_load_schedule / can_switch_company）' AS 检查项;
SELECT column_name, column_type, column_default, column_comment
FROM information_schema.columns
WHERE table_schema = @DB AND table_name = 'mp_schedule_permission'
ORDER BY ordinal_position;