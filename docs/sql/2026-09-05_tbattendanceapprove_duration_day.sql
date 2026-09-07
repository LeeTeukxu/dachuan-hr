-- 审批数据(tbattendanceapprove) 时长增加独立的"天"派生列 durationDay
-- 背景：
--   1) 钉钉请假控件半天等场景，ext_value.durationInHour 返回"日历小时伪值"(如半天=下午固定12h)，
--      而权威展示值在 durationInDay(时长(天)=0.5)。解析修复后按 8 工作小时=1天 折算成小时落库。
--   2) 需求：时长同时持久化"小时"与"天"两个口径，durationDay 恒 = duration(小时)/8，
--      供前端独立"天数"展示/修改列使用，且与统计(读 duration 小时)口径自洽。
-- 幂等：列已存在则跳过；已在 6 个租户(hr_0001~hr_0006)执行。
-- 执行要求：需在全部 hr_XXXX 租户库执行（参照 2026-09-02_tbattendanceapprove_user_id_widen.sql）。

ALTER TABLE `tbattendanceapprove`
    ADD COLUMN `durationDay` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
        DEFAULT NULL COMMENT '时长(天)：与 duration(小时) 一致派生，恒=小时/8' AFTER `durationUnit`;

-- 回填既有数据（口径同写入逻辑：小时→/8、分钟→/60/8、天→原值，保留2位小数）
UPDATE `tbattendanceapprove`
SET `durationDay` = ROUND(
        CASE
            WHEN `durationUnit` = '分钟' THEN CAST(`duration` AS DECIMAL(18,4)) / 60 / 8
            WHEN `durationUnit` = '天' THEN CAST(`duration` AS DECIMAL(18,4))
            ELSE CAST(`duration` AS DECIMAL(18,4)) / 8
        END, 2)
WHERE `durationDay` IS NULL
  AND `duration` IS NOT NULL
  AND `duration` <> ''
  AND `duration` REGEXP '^[0-9]+([.][0-9]+)?$';
