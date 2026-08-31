CREATE TABLE IF NOT EXISTS `hrm_workplan_custom_shift` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shift_name` VARCHAR(100) NOT NULL COMMENT '班次名称',
  `start1` VARCHAR(8) NOT NULL COMMENT '上班时间',
  `end1` VARCHAR(8) NOT NULL COMMENT '下班时间',
  `cross_day` TINYINT NOT NULL DEFAULT 0 COMMENT '是否跨天：0否 1是',
  `shift_hours` INT NOT NULL DEFAULT 0 COMMENT '班次时长（分钟）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workplan_custom_shift_time` (`start1`, `end1`, `cross_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班本地自定义班次表';

ALTER TABLE `tbplanlist`
  ADD COLUMN `shift_source` VARCHAR(20) NULL COMMENT '班次来源：standard/custom' AFTER `CreateTime`,
  ADD COLUMN `custom_shift_id` BIGINT NULL COMMENT '本地自定义班次ID' AFTER `shift_source`;

ALTER TABLE `tbplanlist`
  ADD INDEX `idx_tbplanlist_custom_shift_id` (`custom_shift_id`);
