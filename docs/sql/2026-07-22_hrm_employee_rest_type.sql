-- 员工休息制度字段。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测字段是否存在，已存在则跳过。
-- 取值：1=行政单双休，2=固定月休4天；NULL 表示未维护，按无加班/夜班资格处理。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'rest_type');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `rest_type` INT DEFAULT NULL COMMENT ''休息制度：1行政单双休，2固定月休4天'' AFTER `affiliation_system`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'rest_type');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `rest_type` INT DEFAULT NULL COMMENT ''休息制度：1行政单双休，2固定月休4天'' AFTER `affiliation_system`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'rest_type');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `rest_type` INT DEFAULT NULL COMMENT ''休息制度：1行政单双休，2固定月休4天'' AFTER `affiliation_system`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'rest_type');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `rest_type` INT DEFAULT NULL COMMENT ''休息制度：1行政单双休，2固定月休4天'' AFTER `affiliation_system`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'rest_type');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `rest_type` INT DEFAULT NULL COMMENT ''休息制度：1行政单双休，2固定月休4天'' AFTER `affiliation_system`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
