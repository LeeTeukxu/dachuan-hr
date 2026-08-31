SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_produce_attendance' AND column_name = 'overtime_pay');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_produce_attendance` ADD COLUMN `overtime_pay` DECIMAL(10,2) NULL COMMENT ''加班工资'' AFTER `work_over_time`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_produce_attendance' AND column_name = 'overtime_pay');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_produce_attendance` ADD COLUMN `overtime_pay` DECIMAL(10,2) NULL COMMENT ''加班工资'' AFTER `work_over_time`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_produce_attendance' AND column_name = 'overtime_pay');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_produce_attendance` ADD COLUMN `overtime_pay` DECIMAL(10,2) NULL COMMENT ''加班工资'' AFTER `work_over_time`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_produce_attendance' AND column_name = 'overtime_pay');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_produce_attendance` ADD COLUMN `overtime_pay` DECIMAL(10,2) NULL COMMENT ''加班工资'' AFTER `work_over_time`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_produce_attendance' AND column_name = 'overtime_pay');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_produce_attendance` ADD COLUMN `overtime_pay` DECIMAL(10,2) NULL COMMENT ''加班工资'' AFTER `work_over_time`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
