-- =============================================================================
-- 微信小程序「我的排班」新增数据表
-- 适用数据库：
--   1) 系统库 hr_system（sharding 全局库）  : miniapp_user_binding
--   2) 租户库 hr_{companyId}{suffix}       : hrm_workplan_application
-- 说明：`-- repl 租户库` 标记的行需在每个租户库上重复执行，表名前缀按实际库调整。
-- 用法：先在系统库执行 miniapp_user_binding 段；再对每个租户库执行 hrm_workplan_application 段。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 系统库：小程序用户绑定表（openid → 员工）
--    建议建于工作人员中心库（如 hrsystem 或 sharding DB）。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `miniapp_user_binding` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `openid`       VARCHAR(64)  NOT NULL COMMENT '微信openid',
  `unionid`      VARCHAR(64)  DEFAULT NULL COMMENT '微信unionid（可空）',
  `phone`        VARCHAR(20)  NOT NULL COMMENT '绑定手机号',
  `employee_id`  BIGINT       NOT NULL COMMENT '员工ID(hrm_employee.employee_id)',
  `company_id`   VARCHAR(20)  NOT NULL COMMENT '租户公司ID',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_employee` (`company_id`, `employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信小程序用户绑定';

-- -----------------------------------------------------------------------------
-- 2. 租户库：排班修改申请单
--    需在每个公司库上执行（表前缀与现有 hrm_workplan_custom_shift 等保持一致）。
--    状态流转：pending → approved / rejected / cancelled
--    approved 时 is_current_effective 标记为最新通过的一条。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hrm_workplan_application` (
  `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
  `employee_id`             BIGINT       NOT NULL COMMENT '申请人员工ID',
  `user_id`                 VARCHAR(64)  DEFAULT NULL COMMENT '申请人考勤userId(钉钉)',
  `group_id`                VARCHAR(64)  DEFAULT NULL COMMENT '考勤组ID',
  `work_date`               DATE         NOT NULL COMMENT '申请修改的排班日期',
  `shift_type`              VARCHAR(20)  NOT NULL COMMENT 'standard 标准班 / custom 自定义 / rest 调休休息',
  `class_id`                VARCHAR(32)  DEFAULT NULL COMMENT '标准班次ID(shift_type=standard)',
  `custom_start`            VARCHAR(10)  DEFAULT NULL COMMENT '自定义上班时间 HH:mm',
  `custom_end`              VARCHAR(10)  DEFAULT NULL COMMENT '自定义下班时间 HH:mm',
  `custom_shift_period`     VARCHAR(10)  DEFAULT NULL COMMENT 'day / night',
  `custom_continuous_shift` TINYINT      DEFAULT 0 COMMENT '是否连班(仅白班)',
  `rest_shift_type`         VARCHAR(10)  DEFAULT NULL COMMENT 'adjust 调休 / rest 休息',
  `status`                  VARCHAR(10)  NOT NULL DEFAULT 'pending' COMMENT 'pending待审批/approved通过/rejected驳回/cancelled撤销',
  `is_current_effective`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否当前生效(同日多条已通过时最新通过者为1)',
  `approver_employee_id`    BIGINT       DEFAULT NULL COMMENT '审批人员工ID',
  `approve_time`            DATETIME     DEFAULT NULL COMMENT '审批时间',
  `reject_reason`           VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `remark`                  VARCHAR(255) DEFAULT NULL COMMENT '员工备注',
  `create_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`),
  KEY `idx_status_date` (`status`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班修改申请';