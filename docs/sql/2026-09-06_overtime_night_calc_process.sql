-- 加班/夜班统计明细新增 calc_process 列：记录每日加班/夜班计算过程，明细与汇总悬浮展示。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测列是否存在，已存在则跳过。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'calc_process');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `calc_process` VARCHAR(1000) NULL COMMENT ''加班/夜班计算过程说明'' AFTER `attendance_manual_adjusted`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'calc_process');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `calc_process` VARCHAR(1000) NULL COMMENT ''加班/夜班计算过程说明'' AFTER `attendance_manual_adjusted`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'calc_process');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `calc_process` VARCHAR(1000) NULL COMMENT ''加班/夜班计算过程说明'' AFTER `attendance_manual_adjusted`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'calc_process');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `calc_process` VARCHAR(1000) NULL COMMENT ''加班/夜班计算过程说明'' AFTER `attendance_manual_adjusted`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'calc_process');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `calc_process` VARCHAR(1000) NULL COMMENT ''加班/夜班计算过程说明'' AFTER `attendance_manual_adjusted`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
