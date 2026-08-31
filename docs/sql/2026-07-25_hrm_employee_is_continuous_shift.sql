-- 员工是否连班字段。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测字段是否存在，已存在则跳过。
-- 取值：1=是，2=否；NULL 表示未维护，排班保存时保留原有入参行为。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'is_continuous_shift');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `is_continuous_shift` TINYINT DEFAULT NULL COMMENT ''是否连班：1是，2否'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'is_continuous_shift');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `is_continuous_shift` TINYINT DEFAULT NULL COMMENT ''是否连班：1是，2否'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'is_continuous_shift');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `is_continuous_shift` TINYINT DEFAULT NULL COMMENT ''是否连班：1是，2否'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'is_continuous_shift');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `is_continuous_shift` TINYINT DEFAULT NULL COMMENT ''是否连班：1是，2否'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'is_continuous_shift');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `is_continuous_shift` TINYINT DEFAULT NULL COMMENT ''是否连班：1是，2否'' AFTER `full_attendance`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
