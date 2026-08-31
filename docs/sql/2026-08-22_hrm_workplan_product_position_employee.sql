SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `hrm_workplan_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_name` varchar(100) NOT NULL COMMENT '生产产品名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hrm_workplan_product_sort` (`sort`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班生产产品';

CREATE TABLE IF NOT EXISTS `hrm_workplan_product_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '生产产品ID',
  `position_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hrm_workplan_product_position_product` (`product_id`, `sort`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班生产产品岗位';

CREATE TABLE IF NOT EXISTS `hrm_workplan_position_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `position_id` bigint NOT NULL COMMENT '岗位ID',
  `employee_id` bigint DEFAULT NULL COMMENT '员工ID',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `mobile` varchar(50) DEFAULT NULL COMMENT '手机号',
  `dept_name` varchar(200) DEFAULT NULL COMMENT '部门名称',
  `post` varchar(100) DEFAULT NULL COMMENT '岗位',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hrm_workplan_position_employee_position` (`position_id`, `sort`, `id`),
  KEY `idx_hrm_workplan_position_employee_emp` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班岗位员工';
