-- 修复管理员只授权父级菜单后，系统管理菜单和右上角系统设置齿轮不可见的问题。
-- 使用方式：先切换到目标租户库，例如 USE hr_0003; 再执行本脚本。
-- 默认按 hbadmin 所属角色补授权；如现场管理员账号不同，请修改 @admin_account。

SET @admin_account := 'hbadmin';
SET @admin_role_id := (
    SELECT roleId
    FROM tbloginuser
    WHERE account = @admin_account AND canLogin = 1
    ORDER BY id
    LIMIT 1
);
SET @role_menu_seed := UUID_SHORT();

INSERT INTO tbrolemenu (role_menu_id, role_id, menu_id)
SELECT @role_menu_seed := @role_menu_seed + 1, @admin_role_id, m.id
FROM tbmenu m
WHERE @admin_role_id IS NOT NULL
  AND COALESCE(m.canUse, 1) <> 2
  AND m.path IN (
      '/hrm/system',
      '/hrm/system/loginUser',
      '/hrm/system/rolePermission',
      '/hrm/system/menu',
      '/manage',
      '/manage/insurance-scheme',
      '/manage/vacation',
      '/manage/attendance',
      '/manage/salary'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM tbrolemenu rm
      WHERE rm.role_id = @admin_role_id AND rm.menu_id = m.id
  )
ORDER BY FIELD(m.path,
    '/hrm/system',
    '/hrm/system/loginUser',
    '/hrm/system/rolePermission',
    '/hrm/system/menu',
    '/manage',
    '/manage/insurance-scheme',
    '/manage/vacation',
    '/manage/attendance',
    '/manage/salary'
);

SELECT @admin_account AS admin_account, @admin_role_id AS admin_role_id;
SELECT rm.role_id, m.id, m.pid, m.name, m.path, m.canUse, m.showMenu
FROM tbrolemenu rm
JOIN tbmenu m ON m.id = rm.menu_id
WHERE rm.role_id = @admin_role_id
  AND m.path IN (
      '/hrm/system',
      '/hrm/system/loginUser',
      '/hrm/system/rolePermission',
      '/hrm/system/menu',
      '/manage',
      '/manage/insurance-scheme',
      '/manage/vacation',
      '/manage/attendance',
      '/manage/salary'
  )
ORDER BY m.pid, m.id;
