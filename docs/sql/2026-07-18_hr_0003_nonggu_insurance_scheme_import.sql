-- 2026-07-18 农谷社保医保/公积金方案整理与员工参保方案设置
-- Source: 2026-07 sheets from desktop 社保医保明细 .et and 公积金清单 .xlsx
-- Safety: 4 unmatched source rows are intentionally excluded; see preview report.
USE `hr_0003`;
START TRANSACTION;

INSERT INTO hrm_insurance_scheme (scheme_id, scheme_name, city, level, create_user_id, create_time) VALUES (2078479646634151936, '社保(672.50) 公积金(560.00)', 420882, 0, 1, NOW());
INSERT INTO hrm_insurance_project (project_id, scheme_id, type, project_name, default_amount, corporate_proportion, personal_proportion, corporate_amount, personal_amount, create_user_id, create_time) VALUES
(2078479649268174848, 2078479646634151936, 1, '', 6500.00, 16.00, 8.00, 1040.00, 520.00, 1, NOW()),
(2078479649268174849, 2078479646634151936, 2, '', 6500.00, 8.50, 2.00, 552.50, 130.00, 1, NOW()),
(2078479649268174850, 2078479646634151936, 3, '', 6500.00, 0.70, 0.30, 45.50, 19.50, 1, NOW()),
(2078479649268174851, 2078479646634151936, 4, '', 6500.00, 0.35, 0.00, 22.75, 0.00, 1, NOW()),
(2078479649268174852, 2078479646634151936, 5, '', 6500.00, 0.00, 0.00, 0.00, 0.00, 1, NOW()),
(2078479649268174853, 2078479646634151936, 10, '', 7000.00, 8.00, 8.00, 560.00, 560.00, 1, NOW());

INSERT INTO hrm_insurance_scheme (scheme_id, scheme_name, city, level, create_user_id, create_time) VALUES (2078479646634151937, '社保(466.50) 公积金(158.00)', 420882, 0, 1, NOW());
INSERT INTO hrm_insurance_project (project_id, scheme_id, type, project_name, default_amount, corporate_proportion, personal_proportion, corporate_amount, personal_amount, create_user_id, create_time) VALUES
(2078479649268174854, 2078479646634151937, 1, '', 4500.00, 16.00, 8.00, 720.00, 360.00, 1, NOW()),
(2078479649268174855, 2078479646634151937, 2, '', 4500.00, 8.50, 2.00, 382.50, 90.00, 1, NOW()),
(2078479649268174856, 2078479646634151937, 3, '', 4500.00, 0.70, 0.30, 31.50, 13.50, 1, NOW()),
(2078479649268174857, 2078479646634151937, 4, '', 4500.00, 0.35, 0.00, 15.75, 0.00, 1, NOW()),
(2078479649268174858, 2078479646634151937, 5, '', 4500.00, 0.00, 0.00, 0.00, 0.00, 1, NOW()),
(2078479649268174859, 2078479646634151937, 10, '', 1970.00, 8.00, 8.00, 158.00, 158.00, 1, NOW());

INSERT INTO hrm_insurance_scheme (scheme_id, scheme_name, city, level, create_user_id, create_time) VALUES (2078479646634151938, '社保(672.50) 公积金(158.00)', 420882, 0, 1, NOW());
INSERT INTO hrm_insurance_project (project_id, scheme_id, type, project_name, default_amount, corporate_proportion, personal_proportion, corporate_amount, personal_amount, create_user_id, create_time) VALUES
(2078479649268174860, 2078479646634151938, 1, '', 6500.00, 16.00, 8.00, 1040.00, 520.00, 1, NOW()),
(2078479649268174861, 2078479646634151938, 2, '', 6500.00, 8.50, 2.00, 552.50, 130.00, 1, NOW()),
(2078479649268174862, 2078479646634151938, 3, '', 6500.00, 0.70, 0.30, 45.50, 19.50, 1, NOW()),
(2078479649268174863, 2078479646634151938, 4, '', 6500.00, 0.35, 0.00, 22.75, 0.00, 1, NOW()),
(2078479649268174864, 2078479646634151938, 5, '', 6500.00, 0.00, 0.00, 0.00, 0.00, 1, NOW()),
(2078479649268174865, 2078479646634151938, 10, '', 1970.00, 8.00, 8.00, 158.00, 158.00, 1, NOW());

INSERT INTO hrm_insurance_scheme (scheme_id, scheme_name, city, level, create_user_id, create_time) VALUES (2078479646634151939, '社保(445.80) 公积金(158.00)', 420882, 0, 1, NOW());
INSERT INTO hrm_insurance_project (project_id, scheme_id, type, project_name, default_amount, corporate_proportion, personal_proportion, corporate_amount, personal_amount, create_user_id, create_time) VALUES
(2078479649268174866, 2078479646634151939, 1, '', 4299.00, 16.00, 8.00, 687.84, 343.92, 1, NOW()),
(2078479649268174867, 2078479646634151939, 2, '', 4299.00, 8.50, 2.00, 365.42, 85.98, 1, NOW()),
(2078479649268174868, 2078479646634151939, 3, '', 4299.00, 0.70, 0.30, 30.09, 12.90, 1, NOW()),
(2078479649268174869, 2078479646634151939, 4, '', 4299.00, 0.35, 0.00, 15.05, 0.00, 1, NOW()),
(2078479649268174870, 2078479646634151939, 5, '', 4299.00, 0.00, 0.00, 0.00, 0.00, 1, NOW()),
(2078479649268174871, 2078479646634151939, 10, '', 1970.00, 8.00, 8.00, 158.00, 158.00, 1, NOW());

INSERT INTO hrm_employee_social_security_info (social_security_info_id, employee_id, scheme_id, create_user_id, create_time) VALUES
(2078479649263980544, 2033769831940079658, 2078479646634151937, 1, NOW()),
(2078479649263980545, 2033769831940079627, 2078479646634151937, 1, NOW()),
(2078479649263980546, 2033769831940079628, 2078479646634151937, 1, NOW()),
(2078479649263980547, 1831601326890434643, 2078479646634151937, 1, NOW()),
(2078479649263980548, 2033769831940079633, 2078479646634151939, 1, NOW()),
(2078479649263980549, 2033769831940079634, 2078479646634151939, 1, NOW()),
(2078479649263980550, 1831601326890434640, 2078479646634151939, 1, NOW()),
(2078479649263980551, 2033769831940079644, 2078479646634151939, 1, NOW()),
(2078479649263980552, 2033769831940079645, 2078479646634151939, 1, NOW());

UPDATE hrm_employee_social_security_info SET scheme_id = 2078034513505673217, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434564; -- 张明 13197133302 TYNG-012 社保(466.50) 公积金(320) -> 社保(672.50) 公积金(320.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434566; -- 赵聪 15926668783 TYNG-143 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434565; -- 吴晓霞 13268968337 TYNG-110 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434573; -- 杨太琴 15071911225 TYNG-436 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434569; -- 王琪 15908650550 TYNG-461 社保(356.82) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434568; -- 闫倩 13139361707 TYNG-462 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078034513505673217, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434574; -- 喻艳 13477371019 TYNG-108 社保(466.50) 公积金(320) -> 社保(672.50) 公积金(320.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434576; -- 高文利 15272091137 TYNG-358 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434577; -- 王芳 13872932293 TYNG-393 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 2001501270635024386; -- 李红盼 15827843415 TYNG-463 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434600; -- 苏中心 13093208308 TYNG-026 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434599; -- 倪书成 13581354849 TYNG-058 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434598; -- 许泽刚 13997942772 TYNG-010 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434601; -- 李小瑞 13774052042 TYNG-252 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434603; -- 王敢斌 13797942211 TYNG-311 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434604; -- 黄保红 15871987067 TYNG-316 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434602; -- 张小红 13177179510 TYNG-279 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434605; -- 罗刚 19359824003 TYNG-350 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434606; -- 李冬 13197131292 TYNG-395 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434579; -- 黎冬霜 18872438626 TYNG-134 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434580; -- 严锦 13469766636 TYNG-332 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434581; -- 庞龙斌 18933412821 TYNG-445 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078034513505673217, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434657; -- 闵炎标 13774088433 TYNG-431 社保(445.8) 公积金(320) -> 社保(672.50) 公积金(320.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434658; -- 程佳琪 18986053541 TYNG-434 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434659; -- 时丽平 13451168027 TYNG-435 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434610; -- 曹琴 13477372532 TYNG-055 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434612; -- 范燕东 13986980492 TYNG-088 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434617; -- 康俊 13736367958 TYNG-087 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434616; -- 汪先富 13227165025 TYNG-122 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434609; -- 庞丽红 15608691025 TYNG-176 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434614; -- 王芳 13032750052 TYNG-107 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434618; -- 刘芬芬 18222499648 TYNG-196 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434619; -- 张德艳 15271795054 TYNG-224 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434622; -- 蔡玲玲 13872920980 TYNG-239 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434627; -- 李正军 18373034321 TYNG-341 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434626; -- 孟运香 18672466169 TYNG-337 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434628; -- 张留栓 13068180281 TYNG-360 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434630; -- 蔡军霞 13822101917 TYNG-366 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434632; -- 王庆丰 17764228136 TYNG-394 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434625; -- 舒春霞 15926685624 TYNG-305 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434611; -- 张艳涛 13297137702 TYNG-073 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434621; -- 杨智军 19533567024 TYNG-238 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434620; -- 聂勇军 13774070936 TYNG-236 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434623; -- 赵建礼 13597938165 TYNG-186 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434634; -- 甄景萍 13085183626 TYNG-220 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434633; -- 何辉 13098428068 TYNG-403 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434631; -- 陈玲 15327558001 TYNG-392 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434635; -- 徐勇 18772703095 TYNG-413 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434636; -- 严凤 17786767031 TYNG-417 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434637; -- 马景云 15871988462 TYNG-420 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151938, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434638; -- 孙小虎 18913087894 TYNG-444 社保(466.50) 公积金(144) -> 社保(672.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434641; -- 袁旭东 18872426551 TYNG-441 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434645; -- 丁义勇 13431366893 TYNG-455 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434646; -- 刘柳 18872373756 TYNG-459 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434647; -- 曾运平 18207267883 TYNG-460 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434642; -- 潘国友 13177173368 TYNG-443 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078034513505673217, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434583; -- 王洪平 15771068551 TYNG-064 社保(445.8) 公积金(320) -> 社保(672.50) 公积金(320.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434584; -- 聂小玲 18727593227 TYNG-140 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434585; -- 毛四雄 13451202281 TYNG-159 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434586; -- 马春霞 18971857289 TYNG-231 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434582; -- 余德胜 13872902745 TYNG-009 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434590; -- 张雪梅 15872992452 TYNG-371 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434592; -- 方小荣 18086288036 TYNG-387 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434589; -- 黎勇 13774029675 TYNG-351 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434594; -- 何晓光 15071980390 TYNG-410 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434595; -- 梁爽 13597923212 TYNG-411 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434596; -- 田沂雨 17683894039 TYNG-425 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434597; -- 李国文 13657156482 TYNG-458 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434587; -- 李学飞 18162778161 TYNG-1005 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434591; -- 林艳 15923708983 TYNG-373 社保(466.50) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434652; -- 曾珲 13657159523 TYNG-212 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434653; -- 王金慧 13477570879 TYNG-303 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434654; -- 孙留辉 15872908802 TYNG-309 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434655; -- 郑芬 15071986344 TYNG-287 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434656; -- 单宫廷 15201462668 TYNG-446 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)
UPDATE hrm_employee_social_security_info SET scheme_id = 2078479646634151937, update_user_id = 1, update_time = NOW() WHERE employee_id = 1831601326890434651; -- 范小艳 13477352590 TYNG-021 社保(445.80) 公积金(144) -> 社保(466.50) 公积金(158.00)

SELECT COUNT(*) AS inserted_scheme_count FROM hrm_insurance_scheme WHERE scheme_id IN (2078479646634151936,2078479646634151937,2078479646634151938,2078479646634151939);
SELECT COUNT(*) AS updated_or_inserted_employee_count FROM hrm_employee_social_security_info WHERE employee_id IN (1831601326890434564,1831601326890434566,1831601326890434565,1831601326890434573,1831601326890434569,1831601326890434568,1831601326890434574,1831601326890434576,1831601326890434577,2001501270635024386,1831601326890434600,1831601326890434599,1831601326890434598,1831601326890434601,1831601326890434603,1831601326890434604,1831601326890434602,1831601326890434605,1831601326890434606,2033769831940079658,1831601326890434579,1831601326890434580,1831601326890434581,1831601326890434657,1831601326890434658,1831601326890434659,1831601326890434610,1831601326890434612,2033769831940079627,1831601326890434617,1831601326890434616,1831601326890434609,1831601326890434614,1831601326890434618,1831601326890434619,1831601326890434622,2033769831940079628,1831601326890434627,1831601326890434626,1831601326890434628,1831601326890434630,1831601326890434632,1831601326890434625,1831601326890434611,1831601326890434621,1831601326890434620,1831601326890434623,1831601326890434634,1831601326890434633,1831601326890434631,1831601326890434635,1831601326890434636,1831601326890434637,1831601326890434638,1831601326890434641,1831601326890434643,1831601326890434645,1831601326890434646,1831601326890434647,1831601326890434642,2033769831940079633,2033769831940079634,1831601326890434640,1831601326890434583,1831601326890434584,1831601326890434585,1831601326890434586,1831601326890434582,1831601326890434590,1831601326890434592,1831601326890434589,1831601326890434594,1831601326890434595,1831601326890434596,1831601326890434597,1831601326890434587,1831601326890434591,1831601326890434652,1831601326890434653,1831601326890434654,1831601326890434655,1831601326890434656,1831601326890434651,2033769831940079644,2033769831940079645);
COMMIT;
