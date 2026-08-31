-- =====================================================
-- 排班小程序数据库初始化
-- =====================================================

-- Step 1: 创建系统库绑定表 (hrsystem)
USE hrsystem;

CREATE TABLE IF NOT EXISTS `miniapp_user_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信unionid',
  `phone` VARCHAR(20) NOT NULL COMMENT '绑定手机号',
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `company_id` VARCHAR(10) NOT NULL COMMENT '公司ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_employee` (`employee_id`, `company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户绑定表';

-- Step 2: 创建租户库申请表 (每个 hr_XXXX 库执行)
-- hr_0001
USE hr_0001;
CREATE TABLE IF NOT EXISTS `hrm_workplan_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '申请人员工ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '钉钉userId',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '考勤组ID',
  `work_date` DATE NOT NULL COMMENT '申请修改的日期',
  `shift_type` VARCHAR(20) NOT NULL COMMENT 'standard/custom/rest',
  `class_id` VARCHAR(32) DEFAULT NULL COMMENT '标准班次ID',
  `custom_start` VARCHAR(10) DEFAULT NULL COMMENT '自定义上班时间 HH:mm',
  `custom_end` VARCHAR(10) DEFAULT NULL COMMENT '自定义下班时间 HH:mm',
  `custom_shift_period` VARCHAR(10) DEFAULT NULL COMMENT 'day/night',
  `custom_continuous_shift` TINYINT DEFAULT 0 COMMENT '是否连班',
  `rest_shift_type` VARCHAR(10) DEFAULT NULL COMMENT 'adjust调休/rest休息',
  `status` VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `is_current_effective` TINYINT DEFAULT 0 COMMENT '是否当前生效',
  `approver_employee_id` BIGINT DEFAULT NULL COMMENT '审批人员工ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '员工备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班申请单';

-- hr_0002
USE hr_0002;
CREATE TABLE IF NOT EXISTS `hrm_workplan_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '申请人员工ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '钉钉userId',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '考勤组ID',
  `work_date` DATE NOT NULL COMMENT '申请修改的日期',
  `shift_type` VARCHAR(20) NOT NULL COMMENT 'standard/custom/rest',
  `class_id` VARCHAR(32) DEFAULT NULL COMMENT '标准班次ID',
  `custom_start` VARCHAR(10) DEFAULT NULL COMMENT '自定义上班时间 HH:mm',
  `custom_end` VARCHAR(10) DEFAULT NULL COMMENT '自定义下班时间 HH:mm',
  `custom_shift_period` VARCHAR(10) DEFAULT NULL COMMENT 'day/night',
  `custom_continuous_shift` TINYINT DEFAULT 0 COMMENT '是否连班',
  `rest_shift_type` VARCHAR(10) DEFAULT NULL COMMENT 'adjust调休/rest休息',
  `status` VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `is_current_effective` TINYINT DEFAULT 0 COMMENT '是否当前生效',
  `approver_employee_id` BIGINT DEFAULT NULL COMMENT '审批人员工ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '员工备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班申请单';

-- hr_0003
USE hr_0003;
CREATE TABLE IF NOT EXISTS `hrm_workplan_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '申请人员工ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '钉钉userId',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '考勤组ID',
  `work_date` DATE NOT NULL COMMENT '申请修改的日期',
  `shift_type` VARCHAR(20) NOT NULL COMMENT 'standard/custom/rest',
  `class_id` VARCHAR(32) DEFAULT NULL COMMENT '标准班次ID',
  `custom_start` VARCHAR(10) DEFAULT NULL COMMENT '自定义上班时间 HH:mm',
  `custom_end` VARCHAR(10) DEFAULT NULL COMMENT '自定义下班时间 HH:mm',
  `custom_shift_period` VARCHAR(10) DEFAULT NULL COMMENT 'day/night',
  `custom_continuous_shift` TINYINT DEFAULT 0 COMMENT '是否连班',
  `rest_shift_type` VARCHAR(10) DEFAULT NULL COMMENT 'adjust调休/rest休息',
  `status` VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `is_current_effective` TINYINT DEFAULT 0 COMMENT '是否当前生效',
  `approver_employee_id` BIGINT DEFAULT NULL COMMENT '审批人员工ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '员工备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班申请单';

-- hr_0004
USE hr_0004;
CREATE TABLE IF NOT EXISTS `hrm_workplan_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '申请人员工ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '钉钉userId',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '考勤组ID',
  `work_date` DATE NOT NULL COMMENT '申请修改的日期',
  `shift_type` VARCHAR(20) NOT NULL COMMENT 'standard/custom/rest',
  `class_id` VARCHAR(32) DEFAULT NULL COMMENT '标准班次ID',
  `custom_start` VARCHAR(10) DEFAULT NULL COMMENT '自定义上班时间 HH:mm',
  `custom_end` VARCHAR(10) DEFAULT NULL COMMENT '自定义下班时间 HH:mm',
  `custom_shift_period` VARCHAR(10) DEFAULT NULL COMMENT 'day/night',
  `custom_continuous_shift` TINYINT DEFAULT 0 COMMENT '是否连班',
  `rest_shift_type` VARCHAR(10) DEFAULT NULL COMMENT 'adjust调休/rest休息',
  `status` VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `is_current_effective` TINYINT DEFAULT 0 COMMENT '是否当前生效',
  `approver_employee_id` BIGINT DEFAULT NULL COMMENT '审批人员工ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '员工备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班申请单';

-- hr_0005
USE hr_0005;
CREATE TABLE IF NOT EXISTS `hrm_workplan_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '申请人员工ID',
  `user_id` VARCHAR(64) DEFAULT NULL COMMENT '钉钉userId',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '考勤组ID',
  `work_date` DATE NOT NULL COMMENT '申请修改的日期',
  `shift_type` VARCHAR(20) NOT NULL COMMENT 'standard/custom/rest',
  `class_id` VARCHAR(32) DEFAULT NULL COMMENT '标准班次ID',
  `custom_start` VARCHAR(10) DEFAULT NULL COMMENT '自定义上班时间 HH:mm',
  `custom_end` VARCHAR(10) DEFAULT NULL COMMENT '自定义下班时间 HH:mm',
  `custom_shift_period` VARCHAR(10) DEFAULT NULL COMMENT 'day/night',
  `custom_continuous_shift` TINYINT DEFAULT 0 COMMENT '是否连班',
  `rest_shift_type` VARCHAR(10) DEFAULT NULL COMMENT 'adjust调休/rest休息',
  `status` VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/cancelled',
  `is_current_effective` TINYINT DEFAULT 0 COMMENT '是否当前生效',
  `approver_employee_id` BIGINT DEFAULT NULL COMMENT '审批人员工ID',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '员工备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班申请单';
