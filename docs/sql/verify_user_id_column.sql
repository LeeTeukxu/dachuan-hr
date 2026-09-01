-- 验证 tbattendanceapprove.userId 和 tbattendancerecord.userId 是否已改为 varchar(100)
-- 在生产服务器 MySQL 客户端执行

-- 方法1：直接查 information_schema（推荐，一次看所有租户库）
SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, COLUMN_TYPE 
FROM information_schema.COLUMNS 
WHERE COLUMN_NAME = 'userId' 
  AND TABLE_NAME IN ('tbattendanceapprove', 'tbattendancerecord')
  AND TABLE_SCHEMA LIKE 'hr\_%'
ORDER BY TABLE_SCHEMA, TABLE_NAME;

-- 方法2：如果方法1没结果，逐库检查 hr_0001
USE hr_0001;
DESCRIBE tbattendanceapprove;
DESCRIBE tbattendancerecord;
