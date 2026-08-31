CREATE TABLE IF NOT EXISTS `hrm_bonus_tax_only` (
  `tax_only_bonus_id` bigint NOT NULL COMMENT '只计税奖金ID',
  `employee_id` bigint DEFAULT NULL COMMENT '员工ID',
  `employee_name` varchar(100) DEFAULT NULL COMMENT '员工名称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `bonus` decimal(18,2) DEFAULT NULL COMMENT '只计税奖金金额',
  `year` int DEFAULT NULL COMMENT '年',
  `month` int DEFAULT NULL COMMENT '月',
  PRIMARY KEY (`tax_only_bonus_id`),
  KEY `idx_bonus_tax_only_emp_month` (`employee_id`, `year`, `month`),
  KEY `idx_bonus_tax_only_year_month` (`year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只计税奖金表';


-- 奖金中心拆为两个子菜单：发放奖金上传、只计税奖金上传。
UPDATE tbmenu SET redirect = '/hrm/bonus/payroll' WHERE path = '/hrm/bonus' OR id = 60;
UPDATE tbmenu SET name = '上传奖金(累加至工资计税)', path = '/hrm/bonus/payroll', canUse = 1, showMenu = '1' WHERE id = 600;
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

