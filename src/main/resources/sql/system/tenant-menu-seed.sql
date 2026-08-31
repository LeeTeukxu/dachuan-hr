-- SaaS 改造 P4（可选）：在租户库注册"租户管理"菜单
-- 执行范围：仅超管所在公司（默认 hr_0004）的租户库需要执行，其他租户不应看到该菜单
-- 注意：
--   1. id=2000 为保留 id（1000-1010 已被基础菜单占用，勿用硬编码 1004！）
--   2. tbrolemenu.role_menu_id 为雪花式长 ID，用时间戳生成避免冲突

INSERT INTO tbmenu (id, pid, name, path, canuse, component, redirect, icon, showmenu)
VALUES (2000, 1000, '租户管理', '/hrm/system/tenant', 1, NULL, NULL, NULL, '1')
ON DUPLICATE KEY UPDATE pid=VALUES(pid), path=VALUES(path), canuse=VALUES(canuse), showmenu=VALUES(showmenu);

-- 管理员角色可见（1=管理员 2=系统管理员）
INSERT IGNORE INTO tbrolemenu (role_menu_id, role_id, menu_id)
VALUES ((UNIX_TIMESTAMP(NOW(3))*1000 + 11), 2, 2000),
       ((UNIX_TIMESTAMP(NOW(3))*1000 + 12), 1, 2000);
