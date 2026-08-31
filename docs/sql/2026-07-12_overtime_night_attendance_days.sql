-- 加班/夜班统计明细补充月度应出勤天数与实际出勤天数。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测列是否存在，已存在则跳过。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_days` INT NULL COMMENT ''月度应出勤天数'' AFTER `night_shift_count`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_days` INT NULL COMMENT ''月度实际出勤天数，按每日打卡满8小时统计'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_days` INT NULL COMMENT ''月度应出勤天数'' AFTER `night_shift_count`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_days` INT NULL COMMENT ''月度实际出勤天数，按每日打卡满8小时统计'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_days` INT NULL COMMENT ''月度应出勤天数'' AFTER `night_shift_count`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_days` INT NULL COMMENT ''月度实际出勤天数，按每日打卡满8小时统计'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_days` INT NULL COMMENT ''月度应出勤天数'' AFTER `night_shift_count`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_days` INT NULL COMMENT ''月度实际出勤天数，按每日打卡满8小时统计'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'expected_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `expected_attendance_days` INT NULL COMMENT ''月度应出勤天数'' AFTER `night_shift_count`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail' AND column_name = 'actual_attendance_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_overtime_night_statistics_detail` ADD COLUMN `actual_attendance_days` INT NULL COMMENT ''月度实际出勤天数，按每日打卡满8小时统计'' AFTER `expected_attendance_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
