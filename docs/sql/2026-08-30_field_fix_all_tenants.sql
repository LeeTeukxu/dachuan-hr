-- =====================================================================
-- 2026-08-30 田野农谷反馈字段修复(一次执行版,覆盖 hr_0001~hr_0005)
-- 内容:
--   A. 隐藏重复字段:工作地点(work_address)、生日(birthday)→ is_hidden=1(可逆,数据不删)
--   B. 司龄修复:company_age_start_time=become_time 的行(历史导入污染)改回 entry_time
-- 执行方式(在能连到 192.168.0.26 的机器上):
--   /usr/local/mysql/bin/mysql -h192.168.0.26 -uroot -p < 2026-08-30_field_fix_all_tenants.sql
-- 注意:未来新开的租户(hr_0006+)不再需要此脚本——seed-data.sql 已同步修正;
--       若需对单独某库执行,可使用同目录的两个单库脚本。
-- 执行后核对(逐库):
--   SELECT field_name, is_hidden FROM hrm_employee_field WHERE field_name IN ('work_address','birthday');
--   SELECT COUNT(*) FROM hrm_employee WHERE company_age_start_time = become_time AND become_time IS NOT NULL;
-- =====================================================================

USE `hr_0001`;

UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'work_address' AND name = '工作地点';
UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'birthday' AND name = '生日';
UPDATE hrm_employee SET company_age_start_time = entry_time
 WHERE company_age_start_time = become_time AND become_time IS NOT NULL AND entry_time IS NOT NULL;

USE `hr_0002`;

UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'work_address' AND name = '工作地点';
UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'birthday' AND name = '生日';
UPDATE hrm_employee SET company_age_start_time = entry_time
 WHERE company_age_start_time = become_time AND become_time IS NOT NULL AND entry_time IS NOT NULL;

USE `hr_0003`;

UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'work_address' AND name = '工作地点';
UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'birthday' AND name = '生日';
UPDATE hrm_employee SET company_age_start_time = entry_time
 WHERE company_age_start_time = become_time AND become_time IS NOT NULL AND entry_time IS NOT NULL;

USE `hr_0004`;

UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'work_address' AND name = '工作地点';
UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'birthday' AND name = '生日';
UPDATE hrm_employee SET company_age_start_time = entry_time
 WHERE company_age_start_time = become_time AND become_time IS NOT NULL AND entry_time IS NOT NULL;

USE `hr_0005`;

UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'work_address' AND name = '工作地点';
UPDATE hrm_employee_field SET is_hidden = 1 WHERE field_name = 'birthday' AND name = '生日';
UPDATE hrm_employee SET company_age_start_time = entry_time
 WHERE company_age_start_time = become_time AND become_time IS NOT NULL AND entry_time IS NOT NULL;

