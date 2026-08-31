-- 农谷 2026-06 高温补贴与薪资档案补齐。
-- 生成时间：2026-07-20。
-- 定位要求：使用员工表姓名 + 手机号校验，最终更新对应 employee_id 的考勤汇总/薪资档案。
START TRANSACTION;

-- 潘红琼 19820594095 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389308343406594
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '潘红琼'
  AND e.mobile = '19820594095'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 李凤皇 17573048926 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389308930609154
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '李凤皇'
  AND e.mobile = '17573048926'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 王芳 15871989405 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389309488451585
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '王芳'
  AND e.mobile = '15871989405'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 杨太琴 15071911225 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389310063071233
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '杨太琴'
  AND e.mobile = '15071911225'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 王洪平 15771068551 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389315779907586
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '王洪平'
  AND e.mobile = '15771068551'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 聂小玲 18727593227 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389316572631042
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '聂小玲'
  AND e.mobile = '18727593227'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 毛四雄 13451202281 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389317159833602
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '毛四雄'
  AND e.mobile = '13451202281'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 马春霞 18971857289 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389317902225409
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '马春霞'
  AND e.mobile = '18971857289'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 李学飞 18162778161 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389318447484930
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '李学飞'
  AND e.mobile = '18162778161'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 瞿顺清 13597935193 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389319009521665
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '瞿顺清'
  AND e.mobile = '13597935193'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 黎勇 13774029675 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389319655444481
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '黎勇'
  AND e.mobile = '13774029675'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 张雪梅 15872992452 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389320691437570
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '张雪梅'
  AND e.mobile = '15872992452'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 林艳 15923708983 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389321953923074
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '林艳'
  AND e.mobile = '15923708983'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 方小荣 18086288036 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389322784395266
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '方小荣'
  AND e.mobile = '18086288036'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 胡勇星 15759531811 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389323317071874
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '胡勇星'
  AND e.mobile = '15759531811'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 何晓光 15071980390 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389323866525698
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '何晓光'
  AND e.mobile = '15071980390'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 梁爽 13597923212 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389324600528898
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '梁爽'
  AND e.mobile = '13597923212'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 田沂雨 17683894039 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389325246451713
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '田沂雨'
  AND e.mobile = '17683894039'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 余德胜 13872902745 高温补贴 100.00 补入其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 100.00
WHERE pa.summary_id = 2077389315175927810
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '余德胜'
  AND e.mobile = '13872902745'
  AND COALESCE(pa.other_subsidies, 0) = 0.00;

-- 李红盼 15827843415 岗位工资 2550.00 -> 3370.00
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '3370', sao.update_time = NOW()
WHERE sao.id = 2013823333060071428
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '李红盼'
  AND e.mobile = '15827843415'
  AND CAST(sao.value AS DECIMAL(10,2)) = 2550.00;

-- 喻艳 13477371019 岗位工资 1070.00 -> 1970.00
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '1970', sao.update_time = NOW()
WHERE sao.id = 2013952367488159748
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '喻艳'
  AND e.mobile = '13477371019'
  AND CAST(sao.value AS DECIMAL(10,2)) = 1070.00;

-- 王洪平 15771068551 岗位工资 670.00 -> 1520.00
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '1520', sao.update_time = NOW()
WHERE sao.id = 270473401083236356
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '王洪平'
  AND e.mobile = '15771068551'
  AND CAST(sao.value AS DECIMAL(10,2)) = 670.00;

-- 李学飞 18162778161 岗位工资 2670.00 -> 2970.00
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '2970', sao.update_time = NOW()
WHERE sao.id = 270473392883372036
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '李学飞'
  AND e.mobile = '18162778161'
  AND CAST(sao.value AS DECIMAL(10,2)) = 2670.00;

-- 王志兰 18971843273 基本工资 2130.00 -> 2000.00
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '2000', sao.update_time = NOW()
WHERE sao.id = 270473356334206979
  AND sao.is_pro = 0 AND sao.code = 10101
  AND e.employee_name = '王志兰'
  AND e.mobile = '18971843273'
  AND CAST(sao.value AS DECIMAL(10,2)) = 2130.00;

-- 王志兰 18971843273 岗位工资 1070.00 -> 0.00
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '0', sao.update_time = NOW()
WHERE sao.id = 270473356334206980
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '王志兰'
  AND e.mobile = '18971843273'
  AND CAST(sao.value AS DECIMAL(10,2)) = 1070.00;

COMMIT;
