-- 修复 hr_0003 两名同名“王芳”的钉钉 userId 映射对调问题。
-- 执行前必须先确认预览 SELECT 与当前调查结论一致。

START TRANSACTION;

-- 1. 修复前预览：行政人力资源部王芳当前应错误挂 106860...，生产部王芳当前应错误挂 195726...
SELECT
    e.employee_id,
    e.employee_name,
    e.job_number,
    e.mobile,
    d.name AS dept_name,
    e.dingtalk_user_id,
    au.id AS attendance_user_id,
    au.userId AS attendance_user_id_value,
    au.empId AS attendance_emp_id,
    au.depId AS attendance_dep_id
FROM hr_0003.hrm_employee e
LEFT JOIN hr_0003.hrm_dept d ON d.dept_id = e.dept_id
LEFT JOIN hr_0003.tbattendanceuser au
    ON au.empId = e.employee_id
    OR au.userId COLLATE utf8mb4_0900_ai_ci = e.dingtalk_user_id
WHERE e.employee_id IN (1831601326890434572, 1831601326890434614, 1831601326890434577)
ORDER BY e.employee_id, au.id;

-- 2. 修复员工主表钉钉 ID。条件同时限定 employee_id、手机号和当前错误 userId，避免误伤同名员工。
UPDATE hr_0003.hrm_employee
SET dingtalk_user_id = '1957266823950408',
    update_time = NOW()
WHERE employee_id = 1831601326890434572
  AND employee_name = '王芳'
  AND mobile = '15871989405'
  AND dingtalk_user_id = '1068600027950408';

UPDATE hr_0003.hrm_employee
SET dingtalk_user_id = '1068600027950408',
    update_time = NOW()
WHERE employee_id = 1831601326890434614
  AND employee_name = '王芳'
  AND mobile = '13032750052'
  AND dingtalk_user_id = '1957266823950408';

-- 3. 修复行政人力资源部王芳已有 tbattendanceuser.id=225。
UPDATE hr_0003.tbattendanceuser
SET userId = '1957266823950408',
    userName = '王芳',
    empId = 1831601326890434572,
    depId = 1988144032667267073,
    createMan = COALESCE(createMan, 1)
WHERE id = 225
  AND userId = '1068600027950408'
  AND empId = 1831601326890434572;

-- 4. 补齐生产部王芳 tbattendanceuser 映射。若已存在同 userId 或同 empId 映射，则不插入，需人工复核。
INSERT INTO hr_0003.tbattendanceuser (userId, userName, groupId, empId, depId, createTime, createMan)
SELECT
    '1068600027950408',
    '王芳',
    NULL,
    1831601326890434614,
    1988144261902757889,
    NOW(),
    1
WHERE EXISTS (
    SELECT 1
    FROM hr_0003.hrm_employee
    WHERE employee_id = 1831601326890434614
      AND employee_name = '王芳'
      AND mobile = '13032750052'
      AND dingtalk_user_id = '1068600027950408'
)
AND NOT EXISTS (
    SELECT 1
    FROM hr_0003.tbattendanceuser
    WHERE userId = '1068600027950408'
       OR empId = 1831601326890434614
);

-- 5. 修复后复核：预期行政人力资源部王芳=195726...，生产部王芳=106860...，财务部王芳保持 030568...
SELECT
    e.employee_id,
    e.employee_name,
    e.job_number,
    e.mobile,
    d.name AS dept_name,
    e.dingtalk_user_id,
    au.id AS attendance_user_id,
    au.userId AS attendance_user_id_value,
    au.empId AS attendance_emp_id,
    au.depId AS attendance_dep_id
FROM hr_0003.hrm_employee e
LEFT JOIN hr_0003.hrm_dept d ON d.dept_id = e.dept_id
LEFT JOIN hr_0003.tbattendanceuser au
    ON au.empId = e.employee_id
    OR au.userId COLLATE utf8mb4_0900_ai_ci = e.dingtalk_user_id
WHERE e.employee_id IN (1831601326890434572, 1831601326890434614, 1831601326890434577)
ORDER BY e.employee_id, au.id;

COMMIT;
