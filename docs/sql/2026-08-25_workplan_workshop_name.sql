-- 为tbplanlist表添加workshop_name字段
-- 用于存储车间信息
-- 执行时间：2026-08-25
-- 执行结果：已成功为 hr_0001, hr_0002, hr_0003, hr_0004, hr_0005 添加 workshop_name 字段

SET @schema_name = DATABASE();

-- 检查workshop_name字段是否已存在
SELECT COUNT(*) INTO @column_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'tbplanlist'
  AND COLUMN_NAME = 'workshop_name';

-- 如果字段不存在则添加
SET @sql = IF(@column_exists = 0,
  CONCAT('ALTER TABLE `', @schema_name, '`.`tbplanlist` ADD COLUMN `workshop_name` VARCHAR(200) NULL DEFAULT NULL COMMENT ''车间名称'' AFTER `LinkName`'),
  'SELECT ''workshop_name column already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证字段是否添加成功
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME = 'tbplanlist'
  AND COLUMN_NAME = 'workshop_name';
