-- 基本工资金额设置新增全勤金额与生产体系月休天数。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测字段是否存在，已存在则跳过。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 100.00 COMMENT ''普通员工全勤金额'' AFTER `subsidy`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 500.00 COMMENT ''领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'production_monthly_rest_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `production_monthly_rest_days` INT NOT NULL DEFAULT 4 COMMENT ''生产体系员工月度休息天数'' AFTER `leader_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 100.00 COMMENT ''普通员工全勤金额'' AFTER `subsidy`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 500.00 COMMENT ''领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'production_monthly_rest_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `production_monthly_rest_days` INT NOT NULL DEFAULT 4 COMMENT ''生产体系员工月度休息天数'' AFTER `leader_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 100.00 COMMENT ''普通员工全勤金额'' AFTER `subsidy`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 500.00 COMMENT ''领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'production_monthly_rest_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `production_monthly_rest_days` INT NOT NULL DEFAULT 4 COMMENT ''生产体系员工月度休息天数'' AFTER `leader_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 100.00 COMMENT ''普通员工全勤金额'' AFTER `subsidy`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 500.00 COMMENT ''领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'production_monthly_rest_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `production_monthly_rest_days` INT NOT NULL DEFAULT 4 COMMENT ''生产体系员工月度休息天数'' AFTER `leader_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'ordinary_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `ordinary_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 100.00 COMMENT ''普通员工全勤金额'' AFTER `subsidy`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'leader_full_attendance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `leader_full_attendance_amount` DECIMAL(10,2) NOT NULL DEFAULT 500.00 COMMENT ''领导全勤金额'' AFTER `ordinary_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'production_monthly_rest_days');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `production_monthly_rest_days` INT NOT NULL DEFAULT 4 COMMENT ''生产体系员工月度休息天数'' AFTER `leader_full_attendance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
