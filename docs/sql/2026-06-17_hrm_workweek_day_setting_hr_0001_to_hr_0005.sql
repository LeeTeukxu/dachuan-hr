USE `hr_0001`;
CREATE TABLE IF NOT EXISTS `hrm_workweek_day_setting` (
  `day_setting_id` bigint(20) NOT NULL COMMENT '主键ID',
  `setting_year` int(11) NOT NULL COMMENT '年份',
  `setting_month` tinyint(4) NOT NULL COMMENT '月份 1-12',
  `work_date` date NOT NULL COMMENT '日期',
  `day_type` tinyint(4) NOT NULL COMMENT '日期类型 1上班 2休息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`day_setting_id`),
  UNIQUE KEY `uk_workweek_day_year_date` (`setting_year`,`work_date`),
  KEY `idx_workweek_day_year_month` (`setting_year`,`setting_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单双休月度每日上班休息设置';

USE `hr_0002`;
CREATE TABLE IF NOT EXISTS `hrm_workweek_day_setting` (
  `day_setting_id` bigint(20) NOT NULL COMMENT '主键ID',
  `setting_year` int(11) NOT NULL COMMENT '年份',
  `setting_month` tinyint(4) NOT NULL COMMENT '月份 1-12',
  `work_date` date NOT NULL COMMENT '日期',
  `day_type` tinyint(4) NOT NULL COMMENT '日期类型 1上班 2休息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`day_setting_id`),
  UNIQUE KEY `uk_workweek_day_year_date` (`setting_year`,`work_date`),
  KEY `idx_workweek_day_year_month` (`setting_year`,`setting_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单双休月度每日上班休息设置';

USE `hr_0003`;
CREATE TABLE IF NOT EXISTS `hrm_workweek_day_setting` (
  `day_setting_id` bigint(20) NOT NULL COMMENT '主键ID',
  `setting_year` int(11) NOT NULL COMMENT '年份',
  `setting_month` tinyint(4) NOT NULL COMMENT '月份 1-12',
  `work_date` date NOT NULL COMMENT '日期',
  `day_type` tinyint(4) NOT NULL COMMENT '日期类型 1上班 2休息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`day_setting_id`),
  UNIQUE KEY `uk_workweek_day_year_date` (`setting_year`,`work_date`),
  KEY `idx_workweek_day_year_month` (`setting_year`,`setting_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单双休月度每日上班休息设置';

USE `hr_0004`;
CREATE TABLE IF NOT EXISTS `hrm_workweek_day_setting` (
  `day_setting_id` bigint(20) NOT NULL COMMENT '主键ID',
  `setting_year` int(11) NOT NULL COMMENT '年份',
  `setting_month` tinyint(4) NOT NULL COMMENT '月份 1-12',
  `work_date` date NOT NULL COMMENT '日期',
  `day_type` tinyint(4) NOT NULL COMMENT '日期类型 1上班 2休息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`day_setting_id`),
  UNIQUE KEY `uk_workweek_day_year_date` (`setting_year`,`work_date`),
  KEY `idx_workweek_day_year_month` (`setting_year`,`setting_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单双休月度每日上班休息设置';

USE `hr_0005`;
CREATE TABLE IF NOT EXISTS `hrm_workweek_day_setting` (
  `day_setting_id` bigint(20) NOT NULL COMMENT '主键ID',
  `setting_year` int(11) NOT NULL COMMENT '年份',
  `setting_month` tinyint(4) NOT NULL COMMENT '月份 1-12',
  `work_date` date NOT NULL COMMENT '日期',
  `day_type` tinyint(4) NOT NULL COMMENT '日期类型 1上班 2休息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`day_setting_id`),
  UNIQUE KEY `uk_workweek_day_year_date` (`setting_year`,`work_date`),
  KEY `idx_workweek_day_year_month` (`setting_year`,`setting_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单双休月度每日上班休息设置';
