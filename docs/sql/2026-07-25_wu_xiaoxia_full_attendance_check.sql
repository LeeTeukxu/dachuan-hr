-- 查询吴晓霞的全勤奖相关数据
-- 适用：MySQL 8.0，hr_0003 数据库

-- 1. 查询员工基本信息
SELECT 
    employee_id,
    employee_name,
    status,
    become_time,
    full_attendance,
    expand_production,
    entry_status,
    entry_time,
    probation
FROM hrm_employee 
WHERE employee_id = 1831601326890434565;

-- 2. 查询2026年7月考勤汇总数据
SELECT 
    employee_id,
    stat_year,
    stat_month,
    expected_attendance_days,
    actual_attendance_days,
    accrued_attendance_hours,
    late_minutes,
    absence_days
FROM hrm_attendance_summary 
WHERE employee_id = 1831601326890434565 
  AND stat_year = 2026 
  AND stat_month = 7;

-- 3. 查询2026年7月每日考勤数据
SELECT 
    employee_id,
    work_date,
    late_minutes,
    is_absence,
    leave_type
FROM hrm_attendance_summary_day 
WHERE employee_id = 1831601326890434565 
  AND work_date >= '2026-07-01' 
  AND work_date <= '2026-07-31';

-- 4. 查询2026年7月工资记录中的全勤奖
SELECT 
    employee_id,
    salary_month,
    salary_item_code,
    salary_item_name,
    salary_item_value
FROM salary_month_record 
WHERE employee_id = 1831601326890434565 
  AND salary_month = '2026-07'
  AND salary_item_code = '40102';

-- 5. 查询2026年7月审批数据（请假、加班等）
SELECT 
    user_id,
    work_date,
    approval_type,
    approval_name,
    begin_time,
    end_time,
    duration,
    duration_unit
FROM tbattendanceapprove 
WHERE user_id = '262756571521566047' 
  AND work_date >= '2026-07-01' 
  AND work_date <= '2026-07-31';

-- 6. 查询员工社保信息
SELECT 
    employee_id,
    scheme_id
FROM hrm_employee_social_security_info 
WHERE employee_id = 1831601326890434565;
