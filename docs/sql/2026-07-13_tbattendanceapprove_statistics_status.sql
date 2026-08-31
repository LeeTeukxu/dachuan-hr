-- 审批数据补充统计参与状态。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测列是否存在，已存在则跳过。
-- 空值或空字符串表示参与统计；“取消至统计”表示保留审批快照但不参与加班/夜班统计。

SET @schema_name := 'hr_0001';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tbattendanceapprove' AND column_name = 'statisticsStatus');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`tbattendanceapprove` ADD COLUMN `statisticsStatus` VARCHAR(20) NULL COMMENT ''统计状态：空=参与统计，取消至统计=不参与统计'' AFTER `workDate`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tbattendanceapprove' AND column_name = 'statisticsStatus');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`tbattendanceapprove` ADD COLUMN `statisticsStatus` VARCHAR(20) NULL COMMENT ''统计状态：空=参与统计，取消至统计=不参与统计'' AFTER `workDate`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tbattendanceapprove' AND column_name = 'statisticsStatus');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`tbattendanceapprove` ADD COLUMN `statisticsStatus` VARCHAR(20) NULL COMMENT ''统计状态：空=参与统计，取消至统计=不参与统计'' AFTER `workDate`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tbattendanceapprove' AND column_name = 'statisticsStatus');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`tbattendanceapprove` ADD COLUMN `statisticsStatus` VARCHAR(20) NULL COMMENT ''统计状态：空=参与统计，取消至统计=不参与统计'' AFTER `workDate`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tbattendanceapprove' AND column_name = 'statisticsStatus');
SET @ddl := IF(@has_column = 0, CONCAT('ALTER TABLE `', @schema_name, '`.`tbattendanceapprove` ADD COLUMN `statisticsStatus` VARCHAR(20) NULL COMMENT ''统计状态：空=参与统计，取消至统计=不参与统计'' AFTER `workDate`'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
