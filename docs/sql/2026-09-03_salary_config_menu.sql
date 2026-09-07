-- 2026-09-03 新增计薪设置菜单（对每个租户库执行）
-- 用法：替换 hr_XXXX 为目标租户库名，如 hr_0006

-- 1. 添加菜单项（使用 id=1020 避免冲突）
INSERT INTO hr_XXXX.tbmenu (id, pid, name, path, canuse, component, redirect, icon, showmenu)
VALUES (1020, 1010, '计薪设置', '/manage/salaryConfig', 1, NULL, NULL, NULL, '1')
ON DUPLICATE KEY UPDATE pid=VALUES(pid), path=VALUES(path), canuse=VALUES(canuse), showmenu=VALUES(showmenu);

-- 2. 为管理员角色（role_id=2）授权菜单
INSERT IGNORE INTO hr_XXXX.tbrolemenu (role_menu_id, role_id, menu_id)
VALUES ((UNIX_TIMESTAMP(NOW(3))*1000 + 20), 2, 1020);

-- 验证
SELECT * FROM hr_XXXX.tbmenu WHERE id = 1020;
SELECT * FROM hr_XXXX.tbrolemenu WHERE menu_id = 1020;
