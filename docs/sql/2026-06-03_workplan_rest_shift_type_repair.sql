-- 修复 2026-06-03 排班休息类型迁移半执行的问题。
-- 现象：tbplanlist 已有 custom_continuous_shift，但缺少 rest_shift_type，JPA 查询 tbplanlist 时会报 Unknown column，接口表现为 could not extract ResultSet。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测列是否存在，已存在则跳过。

SET @schema_name := 'hr_0001';
SET @has_column := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'tbplanlist' AND column_name = 'rest_shift_type'
);
SET @ddl := IF(@has_column = 0,
  CONCAT('ALTER TABLE `', @schema_name, '`.`tbplanlist` ADD COLUMN `rest_shift_type` VARCHAR(20) NULL DEFAULT ''adjust'' COMMENT ''休息类排班子类型：adjust调休 rest休息'' AFTER `custom_continuous_shift`'),
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_column := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'tbplanlist' AND column_name = 'rest_shift_type'
);
SET @ddl := IF(@has_column = 0,
  CONCAT('ALTER TABLE `', @schema_name, '`.`tbplanlist` ADD COLUMN `rest_shift_type` VARCHAR(20) NULL DEFAULT ''adjust'' COMMENT ''休息类排班子类型：adjust调休 rest休息'' AFTER `custom_continuous_shift`'),
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_column := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'tbplanlist' AND column_name = 'rest_shift_type'
);
SET @ddl := IF(@has_column = 0,
  CONCAT('ALTER TABLE `', @schema_name, '`.`tbplanlist` ADD COLUMN `rest_shift_type` VARCHAR(20) NULL DEFAULT ''adjust'' COMMENT ''休息类排班子类型：adjust调休 rest休息'' AFTER `custom_continuous_shift`'),
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_column := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'tbplanlist' AND column_name = 'rest_shift_type'
);
SET @ddl := IF(@has_column = 0,
  CONCAT('ALTER TABLE `', @schema_name, '`.`tbplanlist` ADD COLUMN `rest_shift_type` VARCHAR(20) NULL DEFAULT ''adjust'' COMMENT ''休息类排班子类型：adjust调休 rest休息'' AFTER `custom_continuous_shift`'),
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_column := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'tbplanlist' AND column_name = 'rest_shift_type'
);
SET @ddl := IF(@has_column = 0,
  CONCAT('ALTER TABLE `', @schema_name, '`.`tbplanlist` ADD COLUMN `rest_shift_type` VARCHAR(20) NULL DEFAULT ''adjust'' COMMENT ''休息类排班子类型：adjust调休 rest休息'' AFTER `custom_continuous_shift`'),
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
