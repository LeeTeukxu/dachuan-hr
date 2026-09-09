-- 2026-09-09 权限管理模块归拢 + 菜单命名统一
-- 执行位置：租户库（tbmenu / tbroletypes / tbrolemenu 都在 hr_XXXX，不在 hrsystem）
-- 用法：把 {DB} 替换为具体租户库名（如 hr_0001），每个租户库各执行一次。
-- 前置：本脚本只改层级与命名，功能生效还需后端支持多级菜单（TbMenuService / MenuPermissionSupport / CompanyInterceptor 已改递归）。
--
-- 背景：
--   1) tbmenu(1000) 名叫「系统管理」，但前端 router/config.js 里 /hrm/system 分组叫「权限管理」，两处文字不一致；
--   2) 权限类菜单散落在顶级（1030 数据统计 / 5000 排班小程序 / 5020 排班数据加载），权限树里不成组；
--   3) 归拢后会出现三级结构（1000 > 5000 > 5020），后端原实现只支持两层，会导致三级节点丢失、按 path 鉴权失败。

-- 一、命名统一：菜单树里的「系统管理」改为「权限管理」，与前端侧边栏分组一致
UPDATE `{DB}`.`tbmenu` SET name = '权限管理' WHERE id = 1000 AND name <> '权限管理';
-- 看板权限：老库存在「看板权限管理」/「看板权限」两种写法，统一为前端用的「看板权限」
UPDATE `{DB}`.`tbmenu` SET name = '看板权限' WHERE id = 1019 AND name <> '看板权限';
-- 小程序专用开关：加「小程序」前缀，与前端路由里同名的 PC 入口区分开。
-- 1030 原名「数据统计」，与侧边栏第一个入口「数据统计」(/hrm/blank 看板页) 重名，
-- 让人误以为勾了它才该出现那个入口；实际 /hrm/blank 对所有账号默认开放，与 1030 无关。
UPDATE `{DB}`.`tbmenu` SET name = '小程序数据统计' WHERE id = 1030 AND name <> '小程序数据统计';
UPDATE `{DB}`.`tbmenu` SET name = '小程序排班数据加载' WHERE id = 5020 AND name <> '小程序排班数据加载';
-- 5000 保持「排班小程序」：它本身已带小程序字样，且是 5020 的父节点

-- 二、权限项归拢到「权限管理」下（5020 保持为 5000 的子节点，形成第三层）
UPDATE `{DB}`.`tbmenu` SET pid = 1000 WHERE id IN (1030, 5000) AND pid <> 1000;

-- 三、补父节点授权：历史数据里勾了 1030/5000/5020 的角色当时没有 1000，
--    归拢后 1000 成为它们的父，缺 1000 会导致这些菜单整支从用户菜单树中消失（权限丢失）。
SET @i := (SELECT IFNULL(MAX(role_menu_id), 0) FROM `{DB}`.`tbrolemenu`);
INSERT INTO `{DB}`.`tbrolemenu` (role_menu_id, role_id, menu_id)
SELECT @i := @i + 1, t.role_id, 1000
FROM (
    SELECT DISTINCT rm.role_id
    FROM `{DB}`.`tbrolemenu` rm
    WHERE rm.menu_id IN (1030, 5000, 5020)
      AND NOT EXISTS (SELECT 1 FROM `{DB}`.`tbrolemenu` x WHERE x.role_id = rm.role_id AND x.menu_id = 1000)
) t;

-- 四、验证
-- 4.1 菜单层级与命名
SELECT id, pid, name, path FROM `{DB}`.`tbmenu`
WHERE id IN (1000, 1019, 1030, 5000, 5020) ORDER BY id;
-- 期望：1000 权限管理(pid=0)；1019 看板权限(pid=1000)；1030 小程序数据统计(pid=1000)；
--       5000 排班小程序(pid=1000)；5020 小程序排班数据加载(pid=5000)

-- 4.2 不应存在「勾了子孙却没勾 1000」的角色
SELECT r.id, r.name
FROM `{DB}`.`tbrolemenu` rm
JOIN `{DB}`.`tbroletypes` r ON r.id = rm.role_id
WHERE rm.menu_id IN (1030, 5000, 5020)
  AND NOT EXISTS (SELECT 1 FROM `{DB}`.`tbrolemenu` x WHERE x.role_id = rm.role_id AND x.menu_id = 1000)
GROUP BY r.id, r.name;
-- 期望：空结果
