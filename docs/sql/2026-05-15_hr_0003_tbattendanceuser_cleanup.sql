-- 仅清理 hr_0003.tbattendanceuser 已确认失效的 5 条映射
-- 核验依据：
-- 1. 同步用户接口返回的当前有效 userId
-- 2. 钉钉 topapi/v2/user/get 对失效 userId 返回 errcode=60121
-- 3. hrm_employee.dingtalk_user_id 已指向当前有效 userId 的 4 组员工优先按该字段保留
--
-- 注意：
-- - 本脚本刻意只删除 5 条，不处理第 6 条候选失效映射（邓秀强 id=202），以保持和本轮业务要求一致。
-- - 建议先执行 SELECT 复核，再执行 DELETE。

SELECT id, empId, userId, userName, createTime
FROM hr_0003.tbattendanceuser
WHERE id IN (207, 208, 205, 206, 210)
ORDER BY id;

DELETE FROM hr_0003.tbattendanceuser
WHERE id IN (207, 208, 205, 206, 210);

SELECT ROW_COUNT() AS deleted_rows;
