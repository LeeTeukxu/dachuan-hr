-- 加班/夜班统计明细补充人工编辑出勤小时字段。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测列是否存在，已存在则跳过。
-- attendance_manual_adjusted=1 表示应出勤、实际出勤、应计出勤使用人工保存值，查询刷新规则不得覆盖。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度应出勤小时'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度实际出勤小时'' AFTER `actual_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'attendance_manual_adjusted');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `attendance_manual_adjusted` TINYINT NOT NULL DEFAULT 0 COMMENT ''出勤时间是否人工调整：0=自动，1=人工'' AFTER `accrued_attendance_hours`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度应出勤小时'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度实际出勤小时'' AFTER `actual_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'attendance_manual_adjusted');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `attendance_manual_adjusted` TINYINT NOT NULL DEFAULT 0 COMMENT ''出勤时间是否人工调整：0=自动，1=人工'' AFTER `accrued_attendance_hours`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度应出勤小时'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度实际出勤小时'' AFTER `actual_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'attendance_manual_adjusted');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `attendance_manual_adjusted` TINYINT NOT NULL DEFAULT 0 COMMENT ''出勤时间是否人工调整：0=自动，1=人工'' AFTER `accrued_attendance_hours`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度应出勤小时'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度实际出勤小时'' AFTER `actual_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'attendance_manual_adjusted');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `attendance_manual_adjusted` TINYINT NOT NULL DEFAULT 0 COMMENT ''出勤时间是否人工调整：0=自动，1=人工'' AFTER `accrued_attendance_hours`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度应出勤小时'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_hours');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_hours` DECIMAL(10,2) NULL COMMENT ''月度实际出勤小时'' AFTER `actual_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'attendance_manual_adjusted');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `attendance_manual_adjusted` TINYINT NOT NULL DEFAULT 0 COMMENT ''出勤时间是否人工调整：0=自动，1=人工'' AFTER `accrued_attendance_hours`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
