-- 2026-09-06 本地考勤判定引擎 + 补卡规则（每租户库执行）
-- 背景：弃用提交排班到钉钉，本地 tbplanlist 为应出勤基准；判定阈值接入考勤规则设置。
-- 幂等：重复执行无副作用。

-- 1. 考勤规则表增加判定参数与补卡限额
ALTER TABLE hrm_attendance_rule
    ADD COLUMN judge_window_before_minutes INT NULL DEFAULT 120 COMMENT '本地判定：打卡有效窗口提前分钟数',
    ADD COLUMN judge_window_after_minutes INT NULL DEFAULT 240 COMMENT '本地判定：打卡有效窗口延后分钟数',
    ADD COLUMN max_monthly_card_repair INT NULL DEFAULT 3 COMMENT '每月最多补卡次数';

-- 2. 本地考勤判定结果表（每人每日一行，跨天班按上班日记）
CREATE TABLE IF NOT EXISTS hrm_attendance_judge_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    work_date DATE NOT NULL COMMENT '班表日期（跨天班记上班日）',
    rule_id BIGINT NULL COMMENT '判定采用的考勤规则ID',
    should_attend TINYINT(1) NULL COMMENT '是否应出勤',
    shift_type VARCHAR(16) NULL COMMENT '班次类型 standard/custom/rest',
    shift_start VARCHAR(8) NULL COMMENT '班次上班时刻 HH:mm',
    shift_end VARCHAR(8) NULL COMMENT '班次下班时刻 HH:mm（跨天为次日）',
    cross_day TINYINT(1) NULL,
    first_punch_time DATETIME NULL COMMENT '首次有效上班卡',
    last_punch_time DATETIME NULL COMMENT '最后有效下班卡',
    late_minutes INT NULL DEFAULT 0,
    early_minutes INT NULL DEFAULT 0,
    miss_card_count INT NULL DEFAULT 0,
    absenteeism TINYINT(1) NULL,
    rest_day_work TINYINT(1) NULL,
    judge_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_judge_emp_date (emp_id, work_date),
    KEY idx_judge_work_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地考勤判定结果';
