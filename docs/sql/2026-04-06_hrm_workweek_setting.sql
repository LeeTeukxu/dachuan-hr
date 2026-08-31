CREATE TABLE `hrm_workweek_setting` (
  `setting_id` bigint(20) NOT NULL COMMENT '主键ID',
  `setting_year` int(11) NOT NULL COMMENT '年份',
  `week_no` int(11) NOT NULL COMMENT '周次',
  `week_type` tinyint(4) NOT NULL COMMENT '1单休 2双休',
  `week_start_date` date NOT NULL COMMENT '周开始日期',
  `week_end_date` date NOT NULL COMMENT '周结束日期',
  `rest_day_text` varchar(32) NOT NULL COMMENT '休息日描述',
  `manual_override` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否人工改动 0否 1是',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`setting_id`),
  UNIQUE KEY `uk_workweek_year_week` (`setting_year`,`week_no`),
  KEY `idx_workweek_year` (`setting_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度单双休周设置';
