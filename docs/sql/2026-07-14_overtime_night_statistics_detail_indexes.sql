-- 加班/夜班统计明细删除条件补充索引，降低整月重算时的锁等待风险。
-- 适用：MySQL 8.0，多租户库 hr_0001 ~ hr_0005；脚本会先检测同等左前缀索引是否存在，已存在则跳过。

SET @schema_name := 'hr_0001';
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING SUBSTRING_INDEX(GROUP_CONCAT(column_name ORDER BY seq_in_index), ',', 1) = 'work_date'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) LIKE 'employee_id,work_date%'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_employee_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`employee_id`, `work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0002';
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING SUBSTRING_INDEX(GROUP_CONCAT(column_name ORDER BY seq_in_index), ',', 1) = 'work_date'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) LIKE 'employee_id,work_date%'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_employee_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`employee_id`, `work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0003';
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING SUBSTRING_INDEX(GROUP_CONCAT(column_name ORDER BY seq_in_index), ',', 1) = 'work_date'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) LIKE 'employee_id,work_date%'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_employee_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`employee_id`, `work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0004';
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING SUBSTRING_INDEX(GROUP_CONCAT(column_name ORDER BY seq_in_index), ',', 1) = 'work_date'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) LIKE 'employee_id,work_date%'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_employee_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`employee_id`, `work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @schema_name := 'hr_0005';
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING SUBSTRING_INDEX(GROUP_CONCAT(column_name ORDER BY seq_in_index), ',', 1) = 'work_date'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_index := (
  SELECT COUNT(*) FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'hrm_overtime_night_statistics_detail'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) LIKE 'employee_id,work_date%'
  ) matched_indexes
);
SET @ddl := IF(@has_index = 0, CONCAT('CREATE INDEX `idx_otnight_detail_employee_work_date` ON `', @schema_name, '`.`hrm_overtime_night_statistics_detail` (`employee_id`, `work_date`)'), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
