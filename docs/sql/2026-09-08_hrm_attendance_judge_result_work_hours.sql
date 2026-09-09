-- 2026-09-08 本地考勤判定结果表新增「当日出勤工时」列
-- 背景：考勤报表 type=1 字段（钉钉 getcolumnval，占月调用量 14308 次）改为本地计算，
--       「工作时长」列由 HrmAttendanceJudgeServiceImpl 本地算出后落本列，报表不再调钉钉。
-- 影响表：hrm_attendance_judge_result（各 hr_XXXX 租户库）
-- 幂等：可重复执行（已存在该列时跳过）

-- 单库执行（把 hr_0001 换成目标租户库即可）
ALTER TABLE hr_0001.hrm_attendance_judge_result
  ADD COLUMN work_hours DECIMAL(8,2) DEFAULT NULL COMMENT '当日出勤工时(小时)';

-- 缺卡按上/下班分列：考勤汇总(hrm_attendance_report_data 480232655+480232656)会把两列相加，
-- 若两列都填合计会导致缺卡次数翻倍，故判定引擎分别记录。
ALTER TABLE hr_0001.hrm_attendance_judge_result
  ADD COLUMN miss_card_on_count INT DEFAULT 0 COMMENT '上班缺卡次数';
ALTER TABLE hr_0001.hrm_attendance_judge_result
  ADD COLUMN miss_card_off_count INT DEFAULT 0 COMMENT '下班缺卡次数';

-- 全租户批量执行：在任一库执行以下语句生成 ALTER 脚本，再把生成结果拷出来执行（MySQL 8）
-- SELECT CONCAT('ALTER TABLE ', c.table_schema, '.hrm_attendance_judge_result',
--               ' ADD COLUMN ', c.column_name, ' ', c.column_def, ';')
--   FROM (
--     SELECT table_schema, 'work_hours' AS column_name,
--            'DECIMAL(8,2) DEFAULT NULL COMMENT ''当日出勤工时(小时)''' AS column_def
--       FROM information_schema.tables
--      WHERE table_name='hrm_attendance_judge_result' AND table_schema REGEXP '^hr_[0-9]+$'
--      AND table_schema NOT IN (SELECT table_schema FROM information_schema.columns
--                                WHERE table_name='hrm_attendance_judge_result' AND column_name='work_hours')
--     UNION ALL
--     SELECT table_schema, 'miss_card_on_count',
--            'INT DEFAULT 0 COMMENT ''上班缺卡次数'''
--       FROM information_schema.tables
--      WHERE table_name='hrm_attendance_judge_result' AND table_schema REGEXP '^hr_[0-9]+$'
--      AND table_schema NOT IN (SELECT table_schema FROM information_schema.columns
--                                WHERE table_name='hrm_attendance_judge_result' AND column_name='miss_card_on_count')
--     UNION ALL
--     SELECT table_schema, 'miss_card_off_count',
--            'INT DEFAULT 0 COMMENT ''下班缺卡次数'''
--       FROM information_schema.tables
--      WHERE table_name='hrm_attendance_judge_result' AND table_schema REGEXP '^hr_[0-9]+$'
--      AND table_schema NOT IN (SELECT table_schema FROM information_schema.columns
--                                WHERE table_name='hrm_attendance_judge_result' AND column_name='miss_card_off_count')
--   ) c;

-- 说明：新列写入依赖「重算本地判定」（/attendanceData/judgeRecompute）重新计算后才有值；
--       存量行补值方式 = 对目标月份执行一次「重算本地判定」。
