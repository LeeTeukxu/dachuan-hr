SET NAMES utf8mb4;

-- 新增“生产产品”菜单，并把该菜单授权给已经拥有“添加排班”权限的角色。
-- 适用：hr_0001 ~ hr_0005。执行后用户需重新登录刷新 token/menuTree。

USE `hr_0001`;
SET @attendance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @records_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/records' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '生产产品', '/hrm/attendance/workplanProduct', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct');
UPDATE tbmenu
SET pid = @attendance_id, name = '生产产品', canUse = 1, showMenu = '1'
WHERE @attendance_id IS NOT NULL AND path = '/hrm/attendance/workplanProduct';
SET @workplan_product_menu_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @workplan_product_menu_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @records_id) source_roles
WHERE @records_id IS NOT NULL
  AND @workplan_product_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tbrolemenu existing
      WHERE existing.role_id = source_roles.role_id
        AND existing.menu_id = @workplan_product_menu_id
  )
ORDER BY source_roles.role_id;

USE `hr_0002`;
SET @attendance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @records_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/records' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '生产产品', '/hrm/attendance/workplanProduct', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct');
UPDATE tbmenu
SET pid = @attendance_id, name = '生产产品', canUse = 1, showMenu = '1'
WHERE @attendance_id IS NOT NULL AND path = '/hrm/attendance/workplanProduct';
SET @workplan_product_menu_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @workplan_product_menu_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @records_id) source_roles
WHERE @records_id IS NOT NULL
  AND @workplan_product_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tbrolemenu existing
      WHERE existing.role_id = source_roles.role_id
        AND existing.menu_id = @workplan_product_menu_id
  )
ORDER BY source_roles.role_id;

USE `hr_0003`;
SET @attendance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @records_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/records' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '生产产品', '/hrm/attendance/workplanProduct', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct');
UPDATE tbmenu
SET pid = @attendance_id, name = '生产产品', canUse = 1, showMenu = '1'
WHERE @attendance_id IS NOT NULL AND path = '/hrm/attendance/workplanProduct';
SET @workplan_product_menu_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @workplan_product_menu_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @records_id) source_roles
WHERE @records_id IS NOT NULL
  AND @workplan_product_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tbrolemenu existing
      WHERE existing.role_id = source_roles.role_id
        AND existing.menu_id = @workplan_product_menu_id
  )
ORDER BY source_roles.role_id;

USE `hr_0004`;
SET @attendance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @records_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/records' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '生产产品', '/hrm/attendance/workplanProduct', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct');
UPDATE tbmenu
SET pid = @attendance_id, name = '生产产品', canUse = 1, showMenu = '1'
WHERE @attendance_id IS NOT NULL AND path = '/hrm/attendance/workplanProduct';
SET @workplan_product_menu_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @workplan_product_menu_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @records_id) source_roles
WHERE @records_id IS NOT NULL
  AND @workplan_product_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tbrolemenu existing
      WHERE existing.role_id = source_roles.role_id
        AND existing.menu_id = @workplan_product_menu_id
  )
ORDER BY source_roles.role_id;

USE `hr_0005`;
SET @attendance_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance' ORDER BY id LIMIT 1);
SET @records_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/records' ORDER BY id LIMIT 1);
INSERT INTO tbmenu (pid, name, path, canUse, component, redirect, icon, showMenu)
SELECT @attendance_id, '生产产品', '/hrm/attendance/workplanProduct', 1, NULL, NULL, NULL, '1'
WHERE @attendance_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct');
UPDATE tbmenu
SET pid = @attendance_id, name = '生产产品', canUse = 1, showMenu = '1'
WHERE @attendance_id IS NOT NULL AND path = '/hrm/attendance/workplanProduct';
SET @workplan_product_menu_id := (SELECT id FROM tbmenu WHERE path = '/hrm/attendance/workplanProduct' ORDER BY id LIMIT 1);
SET @role_menu_seed := UUID_SHORT();
INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, source_roles.role_id, @workplan_product_menu_id
FROM (SELECT DISTINCT role_id FROM tbrolemenu WHERE menu_id = @records_id) source_roles
WHERE @records_id IS NOT NULL
  AND @workplan_product_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tbrolemenu existing
      WHERE existing.role_id = source_roles.role_id
        AND existing.menu_id = @workplan_product_menu_id
  )
ORDER BY source_roles.role_id;
