ALTER TABLE `hrm_workplan_custom_shift`
  ADD COLUMN `shift_period` VARCHAR(10) NOT NULL DEFAULT 'day' COMMENT '班别：day白班 night夜班' AFTER `cross_day`;

ALTER TABLE `hrm_workplan_custom_shift`
  DROP INDEX `uk_workplan_custom_shift_time`,
  ADD UNIQUE KEY `uk_workplan_custom_shift_time_period` (`start1`, `end1`, `cross_day`, `shift_period`);

ALTER TABLE `tbplanlist`
  ADD COLUMN `custom_shift_period` VARCHAR(10) NULL COMMENT '自定义班别：day白班 night夜班' AFTER `custom_shift_id`;
