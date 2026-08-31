-- 员工级全勤金额字段。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测字段是否存在，已存在则跳过。
-- NULL 表示未设置员工级金额，薪资核算回退最新基本工资金额设置。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级普通员工全勤金额'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级普通员工全勤金额'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级普通员工全勤金额'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级普通员工全勤金额'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级普通员工全勤金额'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''员工级领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
