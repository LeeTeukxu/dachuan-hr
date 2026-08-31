CREATE TABLE IF NOT EXISTS `hrm_attendance_approval_fetch_mark` (
  `fetch_mark_id` BIGINT NOT NULL COMMENT '主键ID',
  `month_key` VARCHAR(7) NOT NULL COMMENT '月份，格式yyyy-MM',
  `user_id` VARCHAR(64) NOT NULL COMMENT '钉钉考勤用户ID',
  `employee_id` BIGINT DEFAULT NULL COMMENT '本地员工ID',
  `approval_type` VARCHAR(32) NOT NULL COMMENT '审批类型维度：all/overtime/misscard/leave/travel',
  `fetch_version` INT NOT NULL DEFAULT 1 COMMENT '审批获取规则版本',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`fetch_mark_id`),
  UNIQUE KEY `uk_approval_fetch_mark_month_user_type_version` (`month_key`, `user_id`, `approval_type`, `fetch_version`),
  KEY `idx_approval_fetch_mark_month_version` (`month_key`, `fetch_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批数据手工获取完成标记表';

SET @has_approval_type := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'hrm_attendance_approval_fetch_mark'
    AND column_name = 'approval_type'
);

SET @ddl := IF(
  @has_approval_type = 0,
  'ALTER TABLE `hrm_attendance_approval_fetch_mark` ADD COLUMN `approval_type` VARCHAR(32) NOT NULL DEFAULT ''all'' COMMENT ''审批类型维度：all/overtime/misscard/leave/travel'' AFTER `employee_id`',
  'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_unique_key := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'hrm_attendance_approval_fetch_mark'
    AND index_name = 'uk_approval_fetch_mark_month_user_version'
);

SET @has_new_unique_key := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'hrm_attendance_approval_fetch_mark'
    AND index_name = 'uk_approval_fetch_mark_month_user_type_version'
);

SET @ddl := CASE
  WHEN @has_old_unique_key > 0 AND @has_new_unique_key = 0 THEN
    'ALTER TABLE `hrm_attendance_approval_fetch_mark` DROP INDEX `uk_approval_fetch_mark_month_user_version`, ADD UNIQUE KEY `uk_approval_fetch_mark_month_user_type_version` (`month_key`, `user_id`, `approval_type`, `fetch_version`)'
  WHEN @has_old_unique_key = 0 AND @has_new_unique_key = 0 THEN
    'ALTER TABLE `hrm_attendance_approval_fetch_mark` ADD UNIQUE KEY `uk_approval_fetch_mark_month_user_type_version` (`month_key`, `user_id`, `approval_type`, `fetch_version`)'
  ELSE
    'SELECT 1'
END;

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
