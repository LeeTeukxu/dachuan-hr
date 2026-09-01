-- 扩大考勤相关表的 userId 列长度，匹配 tbattendanceuser.userId (varchar(100))
-- 问题：钉钉 userId 可达 20+ 字符，原 varchar(20) 导致 Data truncation: Data too long for column 'userId'
-- 影响：审批数据获取失败（HrmAttendanceApprovalSyncServiceImpl.fetchMonthData）
ALTER TABLE `tbattendanceapprove` MODIFY COLUMN `userId` VARCHAR(100) DEFAULT NULL;
ALTER TABLE `tbattendancerecord` MODIFY COLUMN `userId` VARCHAR(100) DEFAULT NULL;
