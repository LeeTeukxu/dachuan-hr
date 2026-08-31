SET NAMES utf8mb4;

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_record' AND column_name = 'include_salary_basic_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` ADD COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` MODIFY COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_record' AND column_name = 'include_salary_basic_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` ADD COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` MODIFY COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_record' AND column_name = 'include_salary_basic_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` ADD COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` MODIFY COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_record' AND column_name = 'include_salary_basic_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` ADD COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` MODIFY COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_record' AND column_name = 'include_salary_basic_insurance_amount');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` ADD COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_record` MODIFY COLUMN `include_salary_basic_insurance_amount` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否累计基本工资金额设置中的长期护理和大额医疗保险金额：0否 1是'' AFTER `corporate_provident_fund_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
