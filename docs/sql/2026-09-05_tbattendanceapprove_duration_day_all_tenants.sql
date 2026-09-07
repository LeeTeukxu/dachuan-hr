-- 2026-09-05 审批数据(tbattendanceapprove) durationDay 列 · 全租户一次性补数脚本
-- 背景：实体 tbattendanceapprove 含 durationDay 字段（时长(天)=小时/8），JPA 查询自动 SELECT 该列；
--       租户库缺列时考勤汇总-下载行政体系考勤报 Unknown column 'tbattendan0_.durationDay'。
-- 用法：在目标 MySQL 实例（含全部 hr_XXXX 租户库）上整库执行一次即可，自动遍历所有 hr_数字 库，幂等可重跑。
-- 单库手工版见同目录 2026-09-05_tbattendanceapprove_duration_day.sql。

USE hrsystem;

DELIMITER $$
DROP PROCEDURE IF EXISTS add_duration_day_all_tenants $$
CREATE PROCEDURE add_duration_day_all_tenants()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE dbName VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT DISTINCT schema_name FROM information_schema.schemata
        WHERE schema_name REGEXP '^hr_[0-9]+$'
        AND EXISTS (SELECT 1 FROM information_schema.tables t
                    WHERE t.table_schema = schema_name AND t.table_name = 'tbattendanceapprove');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO dbName;
        IF done = 1 THEN LEAVE read_loop; END IF;

        -- 1. 缺列才加列（幂等）
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = dbName AND table_name = 'tbattendanceapprove'
                       AND column_name = 'durationDay') THEN
            SET @ddl = CONCAT('ALTER TABLE `', dbName, '`.`tbattendanceapprove` ',
                'ADD COLUMN `durationDay` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ',
                'DEFAULT NULL COMMENT ''时长(天)：与 duration(小时) 一致派生，恒=小时/8'' AFTER `durationUnit`');
            PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;

        -- 2. 回填既有数据（口径同写入逻辑：小时→/8、分钟→/60/8、天→原值，保留2位小数；幂等只补 NULL）
        SET @backfill = CONCAT('UPDATE `', dbName, '`.`tbattendanceapprove` ',
            'SET `durationDay` = ROUND(CASE ',
            'WHEN `durationUnit` = ''分钟'' THEN CAST(`duration` AS DECIMAL(18,4)) / 60 / 8 ',
            'WHEN `durationUnit` = ''天'' THEN CAST(`duration` AS DECIMAL(18,4)) ',
            'ELSE CAST(`duration` AS DECIMAL(18,4)) / 8 END, 2) ',
            'WHERE `durationDay` IS NULL AND `duration` IS NOT NULL AND `duration` <> '''' ',
            'AND `duration` REGEXP ''^[0-9]+([.][0-9]+)?$''');
        PREPARE stmt FROM @backfill; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END $$
DELIMITER ;

CALL add_duration_day_all_tenants();
DROP PROCEDURE add_duration_day_all_tenants;

-- 校验：应输出每个含 tbattendanceapprove 的租户库一行，has_col 全部为 1
SELECT table_schema, COUNT(*) AS has_col
FROM information_schema.columns
WHERE table_name = 'tbattendanceapprove' AND column_name = 'durationDay'
  AND table_schema REGEXP '^hr_[0-9]+$'
GROUP BY table_schema ORDER BY table_schema;
