-- 2026-09-06 修复 ddAccount 表 collation 不一致
-- 现象：租户管理 → 钉钉应用配置"绑定已有租户"列表为空，
--       后台报 Illegal mix of collations (utf8mb4_bg_0900_ai_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT) for operation '='
-- 根因：hrsystem.ddAccount 表（2026-09-04 租户对比功能引入，外部手工建表）
--       的 companyId 列 collation 与 tbcompanylist.companyId 不一致，跨表 JOIN 比较报错。
-- 说明：Java 侧 TenantProvisionService.listDdAccounts() 已加 COLLATE 兜底，此 DDL 为根治，生产需手工执行。

-- 1. 诊断：先确认两表各列的实际字符集与 collation
SELECT table_name, column_name, character_set_name, collation_name
FROM information_schema.columns
WHERE table_schema = 'hrsystem'
  AND table_name IN ('ddAccount', 'tbcompanylist')
  AND column_name = 'companyId';

-- 2. 根治：将 ddAccount 整表转为库基准 collation（若诊断显示是 tbcompanylist 为 bg 侧，则反转目标）
ALTER TABLE `ddAccount` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 3. 验证：重新执行下面查询应不再报错且有数据
SELECT d.companyId, c.companyName, d.appKey
FROM `ddAccount` d LEFT JOIN tbcompanylist c ON d.companyId = c.companyId
ORDER BY d.companyId;
