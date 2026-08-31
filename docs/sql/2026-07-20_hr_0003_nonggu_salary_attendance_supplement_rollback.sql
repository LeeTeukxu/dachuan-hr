-- 回滚：农谷 2026-06 高温补贴与薪资档案补齐。
START TRANSACTION;

-- 回滚 王志兰 18971843273 岗位工资
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '1070', sao.update_time = NOW()
WHERE sao.id = 270473356334206980
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '王志兰'
  AND e.mobile = '18971843273'
  AND CAST(sao.value AS DECIMAL(10,2)) = 0.00;

-- 回滚 王志兰 18971843273 基本工资
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '2130', sao.update_time = NOW()
WHERE sao.id = 270473356334206979
  AND sao.is_pro = 0 AND sao.code = 10101
  AND e.employee_name = '王志兰'
  AND e.mobile = '18971843273'
  AND CAST(sao.value AS DECIMAL(10,2)) = 2000.00;

-- 回滚 李学飞 18162778161 岗位工资
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '2670', sao.update_time = NOW()
WHERE sao.id = 270473392883372036
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '李学飞'
  AND e.mobile = '18162778161'
  AND CAST(sao.value AS DECIMAL(10,2)) = 2970.00;

-- 回滚 王洪平 15771068551 岗位工资
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '670', sao.update_time = NOW()
WHERE sao.id = 270473401083236356
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '王洪平'
  AND e.mobile = '15771068551'
  AND CAST(sao.value AS DECIMAL(10,2)) = 1520.00;

-- 回滚 喻艳 13477371019 岗位工资
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '1070', sao.update_time = NOW()
WHERE sao.id = 2013952367488159748
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '喻艳'
  AND e.mobile = '13477371019'
  AND CAST(sao.value AS DECIMAL(10,2)) = 1970.00;

-- 回滚 李红盼 15827843415 岗位工资
UPDATE hr_0003.hrm_salary_archives_option sao
JOIN hr_0003.hrm_employee e ON e.employee_id = sao.employee_id AND e.is_del = 0
SET sao.value = '2550', sao.update_time = NOW()
WHERE sao.id = 2013823333060071428
  AND sao.is_pro = 0 AND sao.code = 10102
  AND e.employee_name = '李红盼'
  AND e.mobile = '15827843415'
  AND CAST(sao.value AS DECIMAL(10,2)) = 3370.00;

-- 回滚 余德胜 13872902745 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389315175927810
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '余德胜'
  AND e.mobile = '13872902745'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 田沂雨 17683894039 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389325246451713
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '田沂雨'
  AND e.mobile = '17683894039'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 梁爽 13597923212 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389324600528898
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '梁爽'
  AND e.mobile = '13597923212'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 何晓光 15071980390 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389323866525698
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '何晓光'
  AND e.mobile = '15071980390'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 胡勇星 15759531811 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389323317071874
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '胡勇星'
  AND e.mobile = '15759531811'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 方小荣 18086288036 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389322784395266
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '方小荣'
  AND e.mobile = '18086288036'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 林艳 15923708983 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389321953923074
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '林艳'
  AND e.mobile = '15923708983'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 张雪梅 15872992452 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389320691437570
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '张雪梅'
  AND e.mobile = '15872992452'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 黎勇 13774029675 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389319655444481
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '黎勇'
  AND e.mobile = '13774029675'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 瞿顺清 13597935193 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389319009521665
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '瞿顺清'
  AND e.mobile = '13597935193'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 李学飞 18162778161 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389318447484930
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '李学飞'
  AND e.mobile = '18162778161'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 马春霞 18971857289 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389317902225409
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '马春霞'
  AND e.mobile = '18971857289'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 毛四雄 13451202281 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389317159833602
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '毛四雄'
  AND e.mobile = '13451202281'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 聂小玲 18727593227 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389316572631042
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '聂小玲'
  AND e.mobile = '18727593227'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 王洪平 15771068551 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389315779907586
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '王洪平'
  AND e.mobile = '15771068551'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 杨太琴 15071911225 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389310063071233
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '杨太琴'
  AND e.mobile = '15071911225'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 王芳 15871989405 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389309488451585
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '王芳'
  AND e.mobile = '15871989405'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 李凤皇 17573048926 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389308930609154
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '李凤皇'
  AND e.mobile = '17573048926'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

-- 回滚 潘红琼 19820594095 其它补贴
UPDATE hr_0003.hrm_produce_attendance pa
JOIN hr_0003.hrm_employee e ON e.employee_id = pa.employee_id AND e.is_del = 0
SET pa.other_subsidies = 0.00
WHERE pa.summary_id = 2077389308343406594
  AND pa.`year` = 2026 AND pa.`month` = 6
  AND e.employee_name = '潘红琼'
  AND e.mobile = '19820594095'
  AND COALESCE(pa.other_subsidies, 0) = 100.00;

COMMIT;
