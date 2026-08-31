-- 修复社保详情与加班/夜班每日明细内部页权限。
-- 适用：hr_0001 ~ hr_0005。
-- 规则：
-- 1. 已拥有“社保管理”父菜单的角色，补授“社保方案”子菜单，供 /hrm/insurance-scheme/detail 通过 checkPath 继承。
-- 2. 已拥有“考勤管理”父菜单的管理员/系统管理员角色，补授“加班/夜班统计”菜单，供 /overtimeNightDaily 通过 checkPath 继承。

USE `hr_0001`;
SET @insurance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme' ORDER BY id LIMIT 1);
SET @insurance_index_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme/index' ORDER BY id LIMIT 1);
SET @attendance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @overtime_night_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/overtimeNight' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @insurance_index_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @insurance_parent_id) source_roles
WHERE @insurance_index_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @insurance_index_id);
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @overtime_night_id
FROM (
    SELECT DISTINCT rm.role_id
    FROM tbrolemenu rm
    JOIN tbroletypes rt ON rt.id = rm.role_id
    WHERE rm.menu_id = @attendance_parent_id
      AND rt.name IN ('管理员', '系统管理员')
) source_roles
WHERE @overtime_night_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @overtime_night_id);

USE `hr_0002`;
SET @insurance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme' ORDER BY id LIMIT 1);
SET @insurance_index_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme/index' ORDER BY id LIMIT 1);
SET @attendance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @overtime_night_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/overtimeNight' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @insurance_index_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @insurance_parent_id) source_roles
WHERE @insurance_index_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @insurance_index_id);
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @overtime_night_id
FROM (
    SELECT DISTINCT rm.role_id
    FROM tbrolemenu rm
    JOIN tbroletypes rt ON rt.id = rm.role_id
    WHERE rm.menu_id = @attendance_parent_id
      AND rt.name IN ('管理员', '系统管理员')
) source_roles
WHERE @overtime_night_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @overtime_night_id);

USE `hr_0003`;
SET @insurance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme' ORDER BY id LIMIT 1);
SET @insurance_index_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme/index' ORDER BY id LIMIT 1);
SET @attendance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @overtime_night_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/overtimeNight' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @insurance_index_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @insurance_parent_id) source_roles
WHERE @insurance_index_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @insurance_index_id);
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @overtime_night_id
FROM (
    SELECT DISTINCT rm.role_id
    FROM tbrolemenu rm
    JOIN tbroletypes rt ON rt.id = rm.role_id
    WHERE rm.menu_id = @attendance_parent_id
      AND rt.name IN ('管理员', '系统管理员')
) source_roles
WHERE @overtime_night_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @overtime_night_id);

USE `hr_0004`;
SET @insurance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme' ORDER BY id LIMIT 1);
SET @insurance_index_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme/index' ORDER BY id LIMIT 1);
SET @attendance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @overtime_night_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/overtimeNight' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @insurance_index_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @insurance_parent_id) source_roles
WHERE @insurance_index_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @insurance_index_id);
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @overtime_night_id
FROM (
    SELECT DISTINCT rm.role_id
    FROM tbrolemenu rm
    JOIN tbroletypes rt ON rt.id = rm.role_id
    WHERE rm.menu_id = @attendance_parent_id
      AND rt.name IN ('管理员', '系统管理员')
) source_roles
WHERE @overtime_night_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @overtime_night_id);

USE `hr_0005`;
SET @insurance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme' ORDER BY id LIMIT 1);
SET @insurance_index_id := (SELECT id FROM tbmenu WHERE path = '/hrm/insurance-scheme/index' ORDER BY id LIMIT 1);
SET @attendance_parent_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @overtime_night_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/overtimeNight' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @insurance_index_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @insurance_parent_id) source_roles
WHERE @insurance_index_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @insurance_index_id);
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @overtime_night_id
FROM (
    SELECT DISTINCT rm.role_id
    FROM tbrolemenu rm
    JOIN tbroletypes rt ON rt.id = rm.role_id
    WHERE rm.menu_id = @attendance_parent_id
      AND rt.name IN ('管理员', '系统管理员')
) source_roles
WHERE @overtime_night_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbrolemenu existing WHERE existing.role_id = source_roles.role_id AND existing.menu_id = @overtime_night_id);
