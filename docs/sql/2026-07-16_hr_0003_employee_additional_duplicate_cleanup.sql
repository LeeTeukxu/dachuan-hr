-- hr_0003 年度附加扣除历史重复清理脚本
-- 生成日期：2026-07-16
-- 默认 ROLLBACK，不会落库。人工核对无误后，将末尾 ROLLBACK 改为 COMMIT 再执行。
--
-- 背景：
-- 旧年度附加扣除导入逻辑在同一文件包含多个年份时只删除最后一个年份旧数据，
-- 导致 hrm_employee_additional 中残留 2025 年同 employee_id + year 重复。
-- 本脚本清理 2026-07-16 修复王芳错归属后仍存在的 19 组历史重复：
--   - 18 组字段值完全相同，删除重复导入行；
--   - 孙小虎 2025 年两条值不同，按 final_import_excels/专项扣除累加表.xlsx 保留
--     住房贷款利息 12000 + 婴幼儿照护 2000 的新导入行，删除旧错行。

USE hr_0003;
START TRANSACTION;

DELETE FROM hrm_employee_additional
WHERE employee_additional_id IN (
  2077635577948528644,
  2077635577952722945,
  2077635577952722947,
  2077635577952722946,
  2077635577956917249,
  2077635577956917248,
  2077635577956917252,
  2077635577956917251,
  2077635577940140032,
  2077635577944334336,
  2077635577944334337,
  2077635577944334340,
  2077635577944334341,
  2077635577948528640,
  2077635577948528642,
  2077635577944334342,
  271989121063456772,
  2077635577948528643,
  2077635577956917250
);

-- 核对：执行后不应再有 employee_id + year 重复。
SELECT employee_id, year, COUNT(*) AS cnt
FROM hrm_employee_additional
GROUP BY employee_id, year
HAVING COUNT(*) > 1;

-- 核对孙小虎 2025 年保留值。
SELECT employee_id,
       year,
       children_education,
       housing_loan_interest,
       housing_rent,
       supporting_the_elderly,
       continuing_education,
       raising_girls
FROM hrm_employee_additional
WHERE employee_id = 1831601326890434638
  AND year = 2025;

ROLLBACK;
