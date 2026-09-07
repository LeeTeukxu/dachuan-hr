-- 2026-09-06 钉钉主数据同步 + 员工批量设置 + 调薪定薪导入 迁移
-- 执行范围：每个租户库（hr_0001 ~ hr_0005）逐库执行
-- 幂等：可重复执行

-- 1. 员工表新增批量设置字段（watch 迁移）
-- 注意：Oracle 版 MySQL 8 不支持 ADD COLUMN IF NOT EXISTS（MariaDB 语法），
--       此处统一用 information_schema 判断后动态执行，幂等可重复。
SET @col := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hrm_employee' AND COLUMN_NAME = 'is_disabled') = 0,
  'ALTER TABLE hrm_employee ADD COLUMN is_disabled INT NULL COMMENT ''是否残疾 1、是 2、否''', 'SELECT 1'));
PREPARE s1 FROM @col; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @col := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hrm_employee' AND COLUMN_NAME = 'is_retired_soldier') = 0,
  'ALTER TABLE hrm_employee ADD COLUMN is_retired_soldier INT NULL COMMENT ''是否退役军人 1、是 2、否''', 'SELECT 1'));
PREPARE s1 FROM @col; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @col := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hrm_employee' AND COLUMN_NAME = 'is_party_member') = 0,
  'ALTER TABLE hrm_employee ADD COLUMN is_party_member INT NULL COMMENT ''是否党员 1、是 2、否''', 'SELECT 1'));
PREPARE s1 FROM @col; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @col := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hrm_employee' AND COLUMN_NAME = 'personnel_category') = 0,
  'ALTER TABLE hrm_employee ADD COLUMN personnel_category INT NULL COMMENT ''人员分类 1财务 2管理 3技术 4生产 5行政''', 'SELECT 1'));
PREPARE s1 FROM @col; EXECUTE s1; DEALLOCATE PREPARE s1;

-- 2. 部门钉钉同步配置表（排除关键字等）
CREATE TABLE IF NOT EXISTS hrm_dept_sync_config (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键(应用层生成)',
  config_key VARCHAR(100) NOT NULL COMMENT '配置键',
  config_value VARCHAR(1000) NULL COMMENT '配置值',
  update_time DATETIME NULL COMMENT '更新时间',
  UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门钉钉同步配置表';

-- 3. 员工表 dingtalk_user_id 索引（同步按 userid 匹配，该列建表已有则仅补索引）
-- 如列不存在请先执行：ALTER TABLE hrm_employee ADD COLUMN dingtalk_user_id VARCHAR(64) NULL COMMENT '钉钉userId';
SET @stmt = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hrm_employee' AND INDEX_NAME = 'idx_dingtalk_user_id') = 0,
  'ALTER TABLE hrm_employee ADD INDEX idx_dingtalk_user_id (dingtalk_user_id)', 'SELECT 1'));
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
