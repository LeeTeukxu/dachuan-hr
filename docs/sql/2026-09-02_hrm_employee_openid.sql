-- 微信小程序免费登录：员工 OpenID 映射字段。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；字段和索引均为幂等创建。
-- 新增租户也必须执行同等 DDL；openid 允许为空，同一租户内非空值唯一。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'' AFTER `mobile`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @needs_binary := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid' AND (character_set_name <> 'ascii' OR collation_name <> 'ascii_bin'));
SET @ddl := IF(@needs_binary > 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` MODIFY COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'''), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND index_name = 'uk_hrm_employee_openid');
SET @ddl := IF(@has_index = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD UNIQUE KEY `uk_hrm_employee_openid` (`openid`)'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'' AFTER `mobile`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @needs_binary := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid' AND (character_set_name <> 'ascii' OR collation_name <> 'ascii_bin'));
SET @ddl := IF(@needs_binary > 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` MODIFY COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'''), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND index_name = 'uk_hrm_employee_openid');
SET @ddl := IF(@has_index = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD UNIQUE KEY `uk_hrm_employee_openid` (`openid`)'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'' AFTER `mobile`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @needs_binary := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid' AND (character_set_name <> 'ascii' OR collation_name <> 'ascii_bin'));
SET @ddl := IF(@needs_binary > 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` MODIFY COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'''), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND index_name = 'uk_hrm_employee_openid');
SET @ddl := IF(@has_index = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD UNIQUE KEY `uk_hrm_employee_openid` (`openid`)'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'' AFTER `mobile`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @needs_binary := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid' AND (character_set_name <> 'ascii' OR collation_name <> 'ascii_bin'));
SET @ddl := IF(@needs_binary > 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` MODIFY COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'''), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND index_name = 'uk_hrm_employee_openid');
SET @ddl := IF(@has_index = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD UNIQUE KEY `uk_hrm_employee_openid` (`openid`)'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'' AFTER `mobile`'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @needs_binary := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND column_name = 'openid' AND (character_set_name <> 'ascii' OR collation_name <> 'ascii_bin'));
SET @ddl := IF(@needs_binary > 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` MODIFY COLUMN `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''微信小程序openid'''), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'hrm_employee' AND index_name = 'uk_hrm_employee_openid');
SET @ddl := IF(@has_index = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`hrm_employee` ADD UNIQUE KEY `uk_hrm_employee_openid` (`openid`)'), 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
