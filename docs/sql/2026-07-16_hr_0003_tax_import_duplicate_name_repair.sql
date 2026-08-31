-- hr_0003 个税/附加导入重名员工错归属修复脚本
-- 生成日期：2026-07-16
-- 默认 ROLLBACK，不会落库。人工核对无误后，将末尾 ROLLBACK 改为 COMMIT 再执行。
--
-- 背景：
-- 系统旧导入逻辑只按姓名匹配员工，三名“王芳”的部分记录被写入同一 employee_id：
--   TYNG-107 / 13032750052 / 1831601326890434614
--   TYNG-437 / 15871989405 / 1831601326890434572
--   TYNG-393 / 13872932293 / 1831601326890434577
--
-- 本脚本只修正已按原始申报文件和 final_import_excels 核对出的王芳错归属记录。

USE hr_0003;
START TRANSACTION;

-- 1. 个税累计：2026-05 三条王芳记录应按手机号/身份证分配到三名员工。
-- 19281.00 对应 TYNG-437 / 15871989405 / 42243219760108652X。
UPDATE hrm_personal_income_tax
SET employee_id = 1831601326890434572
WHERE personal_income_tax_id = 2077585543773151242
  AND employee_id = 1831601326890434614
  AND year = 2026
  AND end_month = 5
  AND accumulated_income = 19281.00;

-- 32031.00 对应 TYNG-393 / 13872932293 / 42010619790625006X。
UPDATE hrm_personal_income_tax
SET employee_id = 1831601326890434577
WHERE personal_income_tax_id = 2077585543777345539
  AND employee_id = 1831601326890434614
  AND year = 2026
  AND end_month = 5
  AND accumulated_income = 32031.00;

-- 2. 附加累计：赡养老人 15000.00 对应 TYNG-393。
UPDATE hrm_additional
SET employee_id = 1831601326890434577
WHERE additional_id = 2077636115708633089
  AND employee_id = 1831601326890434614
  AND year = 2026
  AND month = 5
  AND supporting_the_elderly = 15000.00;

UPDATE hrm_additional
SET employee_id = 1831601326890434577
WHERE additional_id = 2077635528107614213
  AND employee_id = 1831601326890434614
  AND year = 2026
  AND month = 6
  AND supporting_the_elderly = 15000.00;

-- 3. 年度附加扣除：2025 年 TYNG-393 已有旧记录，补正子女教育后删除错挂到 TYNG-107 的重复行。
UPDATE hrm_employee_additional
SET children_education = 2000.00,
    housing_loan_interest = 0.00,
    housing_rent = 0.00,
    supporting_the_elderly = 3000.00,
    continuing_education = 0.00,
    raising_girls = 0.00
WHERE employee_additional_id = 271989121063456773
  AND employee_id = 1831601326890434577
  AND year = 2025;

DELETE FROM hrm_employee_additional
WHERE employee_additional_id = 2077635577952722948
  AND employee_id = 1831601326890434614
  AND year = 2025
  AND children_education = 2000.00
  AND supporting_the_elderly = 3000.00;

-- 2025 年 TYNG-107 同值重复行保留旧记录，删除导入重复记录。
DELETE FROM hrm_employee_additional
WHERE employee_additional_id = 2077635577944334343
  AND employee_id = 1831601326890434614
  AND year = 2025
  AND children_education = 0.00
  AND supporting_the_elderly = 1500.00;

-- 2026 年赡养老人 3000.00 对应 TYNG-393。
UPDATE hrm_employee_additional
SET employee_id = 1831601326890434577
WHERE employee_additional_id = 2077637286913499138
  AND employee_id = 1831601326890434614
  AND year = 2026
  AND supporting_the_elderly = 3000.00;

-- 核对：以下查询执行后，王芳相关期间不应再出现同一 employee_id 重复。
SELECT employee_id, year, end_month, COUNT(*) AS cnt
FROM hrm_personal_income_tax
WHERE year = 2026
  AND end_month = 5
  AND employee_id IN (1831601326890434614, 1831601326890434572, 1831601326890434577)
GROUP BY employee_id, year, end_month
HAVING COUNT(*) > 1;

SELECT employee_id, year, month, COUNT(*) AS cnt
FROM hrm_additional
WHERE year = 2026
  AND month IN (5, 6)
  AND employee_id IN (1831601326890434614, 1831601326890434572, 1831601326890434577)
GROUP BY employee_id, year, month
HAVING COUNT(*) > 1;

SELECT employee_id, year, COUNT(*) AS cnt
FROM hrm_employee_additional
WHERE year IN (2025, 2026)
  AND employee_id IN (1831601326890434614, 1831601326890434572, 1831601326890434577)
GROUP BY employee_id, year
HAVING COUNT(*) > 1;

ROLLBACK;
