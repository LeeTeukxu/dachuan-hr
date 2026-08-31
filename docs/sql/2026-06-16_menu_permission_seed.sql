-- 权限管理菜单迁移脚本（按现场 tbmenu 现有 ID 安全迁移）
-- 适用现场现状：tbmenu 已有 22 条菜单，id 如 10/20/30/50/60/70 等，path 全部为 NULL。
-- 目标：优先 UPDATE 既有菜单，保留原 id 与 tbrolemenu.menu_id 绑定关系；只 INSERT 缺失的新菜单。
-- 执行前建议备份：tbmenu、tbrolemenu、tbroletypes、tbloginuser。

-- 1. 先补齐既有菜单 path / redirect / icon / showMenu，不改变现有 id。
UPDATE tbmenu SET path = '/hrm/dept', canUse = 1, icon = NULL, showMenu = '1' WHERE id = 10;
UPDATE tbmenu SET path = '/hrm/employee', canUse = 1, icon = NULL, showMenu = '1' WHERE id = 20;
UPDATE tbmenu SET path = '/hrm/attendance', canUse = 1, redirect = '/hrm/attendance/index', icon = 'clock', showMenu = '1' WHERE id = 30;
UPDATE tbmenu SET path = '/hrm/insurance-scheme', canUse = 1, redirect = '/hrm/insurance-scheme/index', icon = NULL, showMenu = '1' WHERE id = 40;
UPDATE tbmenu SET path = '/hrm/salary', canUse = 1, redirect = '/hrm/salary/index', icon = 'WalletFilled', showMenu = '1' WHERE id = 50;
UPDATE tbmenu SET path = '/hrm/bonus', canUse = 1, redirect = '/hrm/bonus/payroll', icon = NULL, showMenu = '1' WHERE id = 60;
UPDATE tbmenu SET path = '/hrm/dataConfig', canUse = 1, redirect = '/hrm/dataConfig/index', icon = NULL, showMenu = '1' WHERE id = 70;

UPDATE tbmenu SET path = '/hrm/attendance/upload', canUse = 1, showMenu = '1' WHERE id = 300;
UPDATE tbmenu SET path = '/hrm/attendance/leave', canUse = 1, showMenu = '0' WHERE id = 310;
UPDATE tbmenu SET path = '/hrm/attendance/summary', canUse = 1, showMenu = '0' WHERE id = 320;
UPDATE tbmenu SET path = '/hrm/attendance/scheduling', canUse = 1, showMenu = '1' WHERE id = 330;
UPDATE tbmenu SET path = '/hrm/attendance/schedulingDetail', canUse = 1, showMenu = '0' WHERE id = 340;
UPDATE tbmenu SET path = '/hrm/attendance/records', canUse = 1, showMenu = '1' WHERE id = 350;
UPDATE tbmenu SET path = '/hrm/attendance/recordsDetail', canUse = 1, showMenu = '0' WHERE id = 360;

UPDATE tbmenu SET path = '/hrm/salary/index', canUse = 1, showMenu = '1' WHERE id = 500;
UPDATE tbmenu SET path = '/hrm/salary/archives', canUse = 1, showMenu = '1' WHERE id = 510;
UPDATE tbmenu SET path = '/hrm/salary/history', canUse = 1, showMenu = '1' WHERE id = 520;
UPDATE tbmenu SET path = '/hrm/salary/tax', canUse = 1, showMenu = '1' WHERE id = 530;
UPDATE tbmenu SET path = '/hrm/salary/addition', canUse = 1, showMenu = '1' WHERE id = 540;
UPDATE tbmenu SET path = '/hrm/salary/additionDeduction', canUse = 1, showMenu = '1' WHERE id = 550;
UPDATE tbmenu SET name = '上传奖金(累加至工资计税)', path = '/hrm/bonus/payroll', canUse = 1, showMenu = '1' WHERE id = 600;
UPDATE tbmenu SET path = '/hrm/dataConfig/index', canUse = 1, showMenu = '1' WHERE id = 700;
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT 60, '上传奖金(只计税)', '/hrm/bonus/taxOnly', 1, NULL, NULL, NULL, '1'
WHERE NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/bonus/taxOnly');
SET @bonus_tax_only_menu_id := (SELECT id FROM tbmenu WHERE path = '/hrm/bonus/taxOnly' ORDER BY id LIMIT 1);
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT UUID_SHORT(), source_roles.role_id, @bonus_tax_only_menu_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = 600) source_roles
WHERE @bonus_tax_only_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM tbrolemenu existing
      WHERE existing.role_id = source_roles.role_id
        AND existing.menu_id = @bonus_tax_only_menu_id
  );



-- 2. 只新增系统管理模块及其子菜单。
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT 0, '系统管理', '/hrm/system', 1, NULL, '/hrm/system/loginUser', 'Tools', '1'
WHERE NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/system');
SET @system_id := (SELECT id FROM tbmenu WHERE path = '/hrm/system' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @system_id, '登录用户管理', '/hrm/system/loginUser', 1, NULL, NULL, NULL, '1'
WHERE @system_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/system/loginUser');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @system_id, '角色权限分配', '/hrm/system/rolePermission', 1, NULL, NULL, NULL, '1'
WHERE @system_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/system/rolePermission');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @system_id, '菜单权限清单', '/hrm/system/menu', 1, NULL, NULL, NULL, '1'
WHERE @system_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/system/menu');

-- 2.1 新增右上角齿轮“系统设置”模块及其子菜单。
-- 该模块不在顶部业务菜单中展示，但作为齿轮入口权限来源；子菜单按具体设置页授权。
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT 0, '系统设置', '/manage', 1, NULL, '/manage/insurance-scheme', 'Tools', '0'
WHERE NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/manage');
SET @manage_id := (SELECT id FROM tbmenu WHERE path = '/manage' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @manage_id, '社保方案管理', '/manage/insurance-scheme', 1, NULL, NULL, NULL, '0'
WHERE @manage_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/manage/insurance-scheme');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @manage_id, '假期管理', '/manage/vacation', 1, NULL, NULL, NULL, '0'
WHERE @manage_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/manage/vacation');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @manage_id, '考勤规则设置', '/manage/attendance', 1, NULL, NULL, NULL, '0'
WHERE @manage_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/manage/attendance');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @manage_id, '基本工资金额设置', '/manage/salary', 1, NULL, NULL, NULL, '0'
WHERE @manage_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/manage/salary');

-- 3. 只新增当前前端已存在、但现场菜单表缺失的业务子菜单。
SET @attendance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '打卡记录', '/hrm/attendance/index', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/index');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '审批数据', '/hrm/attendance/approval', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/approval');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '加班/夜班统计', '/hrm/attendance/overtimeNight', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/overtimeNight');
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '单双休设置', '/hrm/attendance/workweek', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/workweek');

SET @salary_id := (SELECT id FROM tbmenu WHERE path = '/hrm/salary' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @salary_id, '工资条', '/hrm/salary/record', 1, NULL, NULL, NULL, '1'
WHERE @salary_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/salary/record');

SET @insurance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @insurance_id, '社保方案', '/hrm/insurance-scheme/index', 1, NULL, NULL, NULL, '1'
WHERE @insurance_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/insurance-scheme/index');

-- 4. 授权说明：父级菜单不能单独作为可用权限，必须同时授予具体子菜单。
-- 如只授予 `/hrm/system` 或 `/manage` 父级，登录返回的 menuTree 不会包含该入口，前端菜单和右上角齿轮都会隐藏。
-- 首次上线后至少需要一个管理员角色拥有系统管理子菜单权限；如需显示右上角齿轮，还需授予“系统设置”及至少一个设置子菜单。
-- 本脚本不自动给所有角色授权，避免扩大权限；请先确认管理员角色 ID 后，再通过页面或数据库补授以下菜单：
-- SELECT id, name, canUse FROM tbroletypes WHERE canUse = 1 ORDER BY id;
-- SELECT id, pid, name, path FROM tbmenu WHERE path IN ('/hrm/system', '/hrm/system/loginUser', '/hrm/system/rolePermission', '/hrm/system/menu') ORDER BY pid, id;
-- SELECT id, pid, name, path FROM tbmenu WHERE path IN ('/manage', '/manage/insurance-scheme', '/manage/vacation', '/manage/attendance', '/manage/salary') ORDER BY pid, id;
-- 可直接参考/执行 `docs/sql/2026-06-17_grant_system_settings_admin.sql` 为指定管理员账号所属角色补授权。

-- 5. 执行后核验。
SELECT id, pid, name, path, canUse, showMenu FROM tbmenu ORDER BY pid, id;
SELECT path, COUNT(*) AS count FROM tbmenu GROUP BY path HAVING COUNT(*) > 1;
