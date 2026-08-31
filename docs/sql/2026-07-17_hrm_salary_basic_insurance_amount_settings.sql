-- 基本工资金额设置新增社保固定金额。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测字段是否存在，已存在则跳过。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'large_medical_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `large_medical_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 15.00 COMMENT ''大额医疗保险金额'' AFTER `production_monthly_rest_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'long_term_care_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `long_term_care_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 3.00 COMMENT ''长期护理保险金额'' AFTER `large_medical_insurance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'large_medical_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `large_medical_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 15.00 COMMENT ''大额医疗保险金额'' AFTER `production_monthly_rest_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'long_term_care_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `long_term_care_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 3.00 COMMENT ''长期护理保险金额'' AFTER `large_medical_insurance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'large_medical_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `large_medical_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 15.00 COMMENT ''大额医疗保险金额'' AFTER `production_monthly_rest_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'long_term_care_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `long_term_care_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 3.00 COMMENT ''长期护理保险金额'' AFTER `large_medical_insurance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'large_medical_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `large_medical_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 15.00 COMMENT ''大额医疗保险金额'' AFTER `production_monthly_rest_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'long_term_care_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `long_term_care_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 3.00 COMMENT ''长期护理保险金额'' AFTER `large_medical_insurance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'large_medical_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `large_medical_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 15.00 COMMENT ''大额医疗保险金额'' AFTER `production_monthly_rest_days`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_salary_basic' AND column_name = 'long_term_care_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_salary_basic` ADD COLUMN `long_term_care_insurance_amount` DECIMAL(10,2) NOT NULL DEFAULT 3.00 COMMENT ''长期护理保险金额'' AFTER `large_medical_insurance_amount`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
