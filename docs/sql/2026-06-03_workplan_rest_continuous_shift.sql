ALTER TABLE `hrm_workplan_custom_shift`
  ADD COLUMN `continuous_shift` TINYINT NOT NULL DEFAULT 0 COMMENT '是否连班：0否 1是，仅白班有效' AFTER `shift_period`;

ALTER TABLE `hrm_workplan_custom_shift`
  DROP INDEX `uk_workplan_custom_shift_time_period`,
  ADD UNIQUE KEY `uk_workplan_custom_shift_time_period_continuous` (`start1`, `end1`, `cross_day`, `shift_period`, `continuous_shift`);

ALTER TABLE `tbplanlist`
  ADD COLUMN `custom_continuous_shift` TINYINT NULL DEFAULT 0 COMMENT '自定义排班是否连班：0否 1是，仅白班有效' AFTER `custom_shift_period`;

ALTER TABLE `tbplanlist`
  ADD COLUMN `rest_shift_type` VARCHAR(20) NULL DEFAULT 'adjust' COMMENT '休息类排班子类型：adjust调休 rest休息' AFTER `custom_continuous_shift`;
