-- 2026-09-06 排班小程序权限（对每个租户库 + 主库 hrsystem 执行）
-- 用法：把 hr_XXXX 替换为目标租户库名（hr_0001 ~ hr_0005 ...），逐库执行第一段；
--       第二段只在 hrsystem 主库执行一次（tb_api_permission 全局映射）。
-- 执行完主库部分后调用热刷新：POST /hrsystem/tenant/reloadApiPermission（或重启后端）。

-- ============ 第一段：每个租户库 hr_XXXX ============

-- 1. 新表：员工级权限 + 自定义可见明细
CREATE TABLE IF NOT EXISTS hr_XXXX.`mp_schedule_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `employee_id` bigint NOT NULL COMMENT '员工id(hrm_employee.employee_id)',
  `visible_scope` tinyint NOT NULL DEFAULT 1 COMMENT '审批信息可见范围 1=直属下属(默认) 2=全部员工 3=自定义',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排班小程序员工权限';

CREATE TABLE IF NOT EXISTS hr_XXXX.`mp_schedule_visible_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_employee_id` bigint NOT NULL COMMENT '配置的员工id',
  `visible_employee_id` bigint NOT NULL COMMENT '可见其申请的员工id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`permission_employee_id`,`visible_employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排班小程序审批可见范围-自定义明细';

-- 2. 菜单：1021 PC 配置页入口（系统管理下）；5000/5020 隐藏虚拟节点（租户级开关：排班数据加载）
INSERT INTO hr_XXXX.tbmenu (id, pid, name, path, canuse, component, redirect, icon, showmenu)
VALUES (1021, 1000, '排班小程序权限', '/hrm/system/miniappPermission', 1, NULL, NULL, NULL, '1')
ON DUPLICATE KEY UPDATE pid=VALUES(pid), path=VALUES(path), canuse=VALUES(canuse), showmenu=VALUES(showmenu);
INSERT INTO hr_XXXX.tbmenu (id, pid, name, path, canuse, component, redirect, icon, showmenu)
VALUES (5000, 0, '排班小程序', '/miniapp/schedule', 1, NULL, NULL, NULL, '0')
ON DUPLICATE KEY UPDATE pid=VALUES(pid), path=VALUES(path), canuse=VALUES(canuse), showmenu=VALUES(showmenu);
INSERT INTO hr_XXXX.tbmenu (id, pid, name, path, canuse, component, redirect, icon, showmenu)
VALUES (5020, 5000, '排班数据加载', '/miniapp/schedule/load', 1, NULL, NULL, NULL, '0')
ON DUPLICATE KEY UPDATE pid=VALUES(pid), path=VALUES(path), canuse=VALUES(canuse), showmenu=VALUES(showmenu);

-- 3. 角色映射：系统管理员(2)勾全部；所有已有角色默认勾"排班数据加载"(5020)——默认勾选
INSERT IGNORE INTO hr_XXXX.tbrolemenu (role_menu_id, role_id, menu_id)
VALUES (2068000000000000101, 2, 1021), (2068000000000000102, 2, 5000), (2068000000000000103, 2, 5020);
INSERT IGNORE INTO hr_XXXX.tbrolemenu (role_menu_id, role_id, menu_id)
SELECT 2068000000000001000 + t.id * 10, t.id, 5020
FROM hr_XXXX.tbroletypes t
WHERE t.canUse = 1
  AND NOT EXISTS (SELECT 1 FROM hr_XXXX.tbrolemenu rm WHERE rm.role_id = t.id AND rm.menu_id = 5020);

-- 验证
SELECT * FROM hr_XXXX.tbmenu WHERE id IN (1021, 5000, 5020);
SELECT rm.role_id, rm.menu_id FROM hr_XXXX.tbrolemenu rm WHERE rm.menu_id IN (1021, 5000, 5020) ORDER BY rm.role_id;

-- ============ 第二段：主库 hrsystem（只执行一次）============

INSERT INTO hrsystem.tb_api_permission (api_prefix, menu_paths, match_type, sortno, remark) VALUES
('/mp/mySchedule','/miniapp/schedule/load','exact',930,'小程序-排班数据加载'),
('/mp/mySchedule/day','/miniapp/schedule/load','exact',931,'小程序-排班数据加载(单日)'),
('/mp/schedule/query','/miniapp/schedule/load','exact',932,'小程序-排班查询'),
('/mpPermission','/hrm/system/miniappPermission','prefix',933,'排班小程序权限配置页')
ON DUPLICATE KEY UPDATE menu_paths=VALUES(menu_paths), match_type=VALUES(match_type);
