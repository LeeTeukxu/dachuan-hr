-- 2026-09-10 部门编制字段（4 部门统计编制真实化）
-- 背景：
--   hrm_dept 表无"编制"字段，原 deptStatistics 用示例值（前端硬编）显示缺超编；
--   现加 plan_num 字段，由 PC 部门维护页维护真实编制数，部门统计按
--   在职人数 vs plan_num 计算缺编/超编/已满角标。
-- 执行范围：每个租户库 hr_XXXX 执行一次（幂等：列已存在则跳过）。
-- 后续步骤：
--   1. HrmDept entity + getter/setter 加 plan_num
--   2. 部门维护 Controller/Service 保存 plan_num
--   3. /mp/dashboard/deptOverview 返回 plan_num 与在职人数，miniapp 据此算缺/超/已满
--   4. miniapp deptStatistics.vue 恢复缺编/超编/已满角标

-- ---------- 1. 字段（幂等） ----------
SET @DB = '{DB}';

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @DB AND table_name = 'hrm_dept'
                 AND column_name = 'plan_num');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `{DB}`.`hrm_dept` ADD COLUMN `plan_num` INT NULL DEFAULT NULL COMMENT ''部门编制人数(管理员维护)；NULL=未配置，按真实在职显示'' AFTER `code`',
  'SELECT ''plan_num 已存在，跳过''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 验证 ----------
SELECT 'hrm_dept.plan_num' AS 检查项;
SELECT column_name, column_type, column_default, column_comment
FROM information_schema.columns
WHERE table_schema = @DB AND table_name = 'hrm_dept' AND column_name = 'plan_num';