-- =====================================================================
-- 2026-09-09 小程序权限改为「员工级」+ 权限菜单树合并
-- 背景：
--   1) /mp/dashboard/*（数据统计）、/mp/mySchedule*、/mp/schedule/query（排班数据加载）
--      原先挂菜单 /hrm/dataStatistics、/miniapp/schedule/load，按“租户内任一角色勾选”
--      判定 → 粒度是公司，与“小程序是员工登录”不符。
--   2) 权限菜单树里同时存在 1021 排班小程序权限 / 1030 小程序数据统计 /
--      5000 排班小程序 / 5020 小程序排班数据加载，四选一格局混乱。
-- 方案（已与用户确认）：
--   - 菜单树只保留 1021，改名「小程序权限」（= PC 配置页入口，角色勾选=谁能进这个页面）
--   - 1030 / 5000 / 5020 三条菜单及其角色授权全部删除
--   - 三个能力（添加排班 / 数据统计 / 排班数据加载）下沉到 mp_schedule_permission
--     按员工配置；未配置的员工一律无权限（严格模式）
-- 执行范围：
--   第 1~3 段：每个租户库各执行一次（tbmenu / tbroletypes / tbrolemenu /
--              mp_schedule_permission 都在租户库 hr_XXXX）
--   第 4 段：系统库 hrsystem 执行一次
-- =====================================================================

-- ---------- 1. 员工级能力字段（幂等：列已存在则跳过） ----------
SET @DB = '{DB}';

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @DB AND table_name = 'mp_schedule_permission'
                 AND column_name = 'can_schedule');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `{DB}`.`mp_schedule_permission` ADD COLUMN `can_schedule` tinyint NOT NULL DEFAULT 0 COMMENT ''可添加排班 0=否 1=是'' AFTER `visible_scope`',
  'SELECT ''can_schedule 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @DB AND table_name = 'mp_schedule_permission'
                 AND column_name = 'can_view_statistics');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `{DB}`.`mp_schedule_permission` ADD COLUMN `can_view_statistics` tinyint NOT NULL DEFAULT 0 COMMENT ''小程序数据统计 0=否 1=是'' AFTER `can_schedule`',
  'SELECT ''can_view_statistics 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @DB AND table_name = 'mp_schedule_permission'
                 AND column_name = 'can_load_schedule');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `{DB}`.`mp_schedule_permission` ADD COLUMN `can_load_schedule` tinyint NOT NULL DEFAULT 0 COMMENT ''小程序排班数据加载 0=否 1=是'' AFTER `can_view_statistics`',
  'SELECT ''can_load_schedule 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 存量记录兼容：旧语义“有记录 = 可添加排班”，迁移后保持可添加排班不变
UPDATE `{DB}`.`mp_schedule_permission` SET can_schedule = 1 WHERE can_schedule = 0;

-- ---------- 2. 权限菜单树合并 ----------
-- 1021 改名（页面不再只管排班）
UPDATE `{DB}`.`tbmenu` SET name = '小程序权限' WHERE id = 1021 AND name <> '小程序权限';

-- 删除三条“公司级开关”菜单的角色授权与菜单行
DELETE FROM `{DB}`.`tbrolemenu` WHERE menu_id IN (1030, 5000, 5020);
DELETE FROM `{DB}`.`tbmenu`     WHERE id     IN (1030, 5000, 5020);

-- ---------- 3. 验证（租户库） ----------
SELECT '菜单（应只剩 1021 小程序权限，无 1030/5000/5020）' AS 检查项;
SELECT id, pid, name, path FROM `{DB}`.`tbmenu` WHERE id IN (1021, 1030, 5000, 5020) ORDER BY id;

SELECT '残留角色授权（应为空）' AS 检查项;
SELECT * FROM `{DB}`.`tbrolemenu` WHERE menu_id IN (1030, 5000, 5020);

SELECT '员工权限表字段' AS 检查项;
SELECT column_name, column_default, column_comment FROM information_schema.columns
WHERE table_schema = @DB AND table_name = 'mp_schedule_permission'
  AND column_name IN ('can_schedule','can_view_statistics','can_load_schedule');

-- =====================================================================
-- 4. 系统库 hrsystem：清理已作废的 /mp 菜单映射（整个环境执行一次）
--    这些接口改由员工级能力判定，不再挂菜单；保留 /mpPermission → miniappPermission
-- =====================================================================
DELETE FROM hrsystem.tb_api_permission
WHERE api_prefix IN ('/mp/mySchedule', '/mp/mySchedule/day', '/mp/schedule/query')
   OR api_prefix LIKE '/mp/dashboard%';

SELECT '已作废的 /mp 菜单映射（应为空）' AS 检查项;
SELECT api_prefix, menu_paths FROM hrsystem.tb_api_permission
WHERE api_prefix LIKE '/mp/dashboard%' OR api_prefix IN ('/mp/mySchedule','/mp/mySchedule/day','/mp/schedule/query');

SELECT '保留的 PC 配置页映射（应有 1 条）' AS 检查项;
SELECT api_prefix, menu_paths FROM hrsystem.tb_api_permission WHERE api_prefix = '/mpPermission';
