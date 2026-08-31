SET NAMES utf8mb4;

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_project' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_project_record' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_project' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_project_record' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_project' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_project_record' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_project' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_project_record' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_project' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_project` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_insurance_month_emp_project_record' AND column_name = 'is_enabled');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'), CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_insurance_month_emp_project_record` MODIFY COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用：0禁用 1启用'' AFTER `personal_amount`'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
