-- 补充 /tenant/list、/tenant/tables、/tenant/reloadApiPermission 的 API→菜单权限映射
-- 修复：行政经理等有菜单权限的角色访问租户管理页面被 checkProvisionPermission 误拦
INSERT INTO tb_api_permission (api_prefix, menu_paths, match_type, sortno, remark) VALUES
('/tenant/list','/hrm/dataConfig/index','prefix',801,'租户列表'),
('/tenant/tables','/hrm/dataConfig/index','prefix',802,'租户表清单'),
('/tenant/reloadApiPermission','/hrm/dataConfig/index','prefix',803,'刷新权限映射')
ON DUPLICATE KEY UPDATE menu_paths=VALUES(menu_paths), match_type=VALUES(match_type);
