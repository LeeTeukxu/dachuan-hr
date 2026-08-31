-- hr_0003 李明明个税/附加同名错归属修复脚本
-- 生成日期：2026-07-17
-- 默认 ROLLBACK，不会落库。人工核对无误后，将末尾 ROLLBACK 改为 COMMIT 再执行。
--
-- 背景：
-- 在职李明明：
--   employee_id = 1831601326890434563
--   mobile      = 17389819211
--   job_number  = 1718539734976
-- 已删除同名李明明：
--   employee_id = 2033769831940079620
--   mobile      = 18889692758
--
-- 用户提供的 final_import_excels 中，个税累计、附加累计和年度附加配置均使用
-- 在职李明明手机号 17389819211；当前数据库中相关数据错挂在已删除同名员工上。

USE hr_0003;
START TRANSACTION;

-- 1. 删除在职李明明当前错误重算出的 2026-06 个税累计。
-- 该记录以上月累计 0、累计减除费用 60000 生成，不能作为 7 月或后续核算基础。
DELETE FROM hrm_personal_income_tax
WHERE personal_income_tax_id = 2078065280805888002
  AND employee_id = 1831601326890434563
  AND year = 2026
  AND end_month = 6
  AND accumulated_income = 5850.00
  AND accumulated_deduction_of_expenses = 60000.00
  AND accumulated_provident_fund = 992.50
  AND accumulated_tax_payment = 0.00;

-- 2. 将 2026-05 上月个税累计从已删除同名员工迁回在职李明明。
UPDATE hrm_personal_income_tax
SET employee_id = 1831601326890434563
WHERE personal_income_tax_id = 2077585543773151237
  AND employee_id = 2033769831940079620
  AND year = 2026
  AND end_month = 5
  AND accumulated_income = 58594.00
  AND accumulated_deduction_of_expenses = 25000.00
  AND accumulated_provident_fund = 4344.50
  AND accumulated_tax_payment = 213.99;

-- 3. 将 2026-05/06/07 附加累计从已删除同名员工迁回在职李明明。
UPDATE hrm_additional
SET employee_id = 1831601326890434563
WHERE additional_id = 2077636115700244481
  AND employee_id = 2033769831940079620
  AND year = 2026
  AND month = 5
  AND children_education = 10000.00
  AND housing_rent = 4000.00
  AND supporting_the_elderly = 7500.00;

UPDATE hrm_additional
SET employee_id = 1831601326890434563
WHERE additional_id = 2077635528107614209
  AND employee_id = 2033769831940079620
  AND year = 2026
  AND month = 6
  AND children_education = 10000.00
  AND housing_rent = 4000.00
  AND supporting_the_elderly = 7500.00;

UPDATE hrm_additional
SET employee_id = 1831601326890434563
WHERE additional_id = 2078016398927339522
  AND employee_id = 2033769831940079620
  AND year = 2026
  AND month = 7
  AND children_education = 12000.00
  AND housing_rent = 4800.00
  AND supporting_the_elderly = 9000.00;

-- 4. 将 2026 年年度专项附加扣除配置迁回在职李明明。
UPDATE hrm_employee_additional
SET employee_id = 1831601326890434563
WHERE employee_additional_id = 2077637286909304834
  AND employee_id = 2033769831940079620
  AND year = 2026
  AND children_education = 2000.00
  AND housing_rent = 800.00
  AND supporting_the_elderly = 1500.00;

-- 5. 核对：在职李明明应拥有 2026-05 个税累计；已删除同名员工不应再有 2026-05/06 目标数据。
SELECT p.personal_income_tax_id,
       p.employee_id,
       e.employee_name,
       e.mobile,
       e.is_del,
       p.year,
       p.end_month,
       p.accumulated_income,
       p.accumulated_deduction_of_expenses,
       p.accumulated_provident_fund,
       p.accumulated_tax_payment
FROM hrm_personal_income_tax p
LEFT JOIN hrm_employee e ON e.employee_id = p.employee_id
WHERE p.year = 2026
  AND p.end_month IN (5, 6)
  AND p.employee_id IN (1831601326890434563, 2033769831940079620)
ORDER BY p.end_month, p.employee_id;

SELECT a.additional_id,
       a.employee_id,
       e.employee_name,
       e.mobile,
       e.is_del,
       a.year,
       a.month,
       a.children_education,
       a.housing_rent,
       a.housing_loan_interest,
       a.supporting_the_elderly,
       a.continuing_education,
       a.raising_girls
FROM hrm_additional a
LEFT JOIN hrm_employee e ON e.employee_id = a.employee_id
WHERE a.year = 2026
  AND a.month IN (5, 6, 7)
  AND a.employee_id IN (1831601326890434563, 2033769831940079620)
ORDER BY a.month, a.employee_id;

SELECT ea.employee_additional_id,
       ea.employee_id,
       e.employee_name,
       e.mobile,
       e.is_del,
       ea.year,
       ea.children_education,
       ea.housing_rent,
       ea.housing_loan_interest,
       ea.supporting_the_elderly,
       ea.continuing_education,
       ea.raising_girls
FROM hrm_employee_additional ea
LEFT JOIN hrm_employee e ON e.employee_id = ea.employee_id
WHERE ea.year = 2026
  AND ea.employee_id IN (1831601326890434563, 2033769831940079620)
ORDER BY ea.employee_id, ea.employee_additional_id;

ROLLBACK;
