-- =====================================================
-- 2026-09-10 小程序统计「公司切换」可见公司授权表（系统库 hrsystem）
-- B方案：员工在「数据统计」页可通过公司chip切换到其可见的其它公司看板。
-- 授权键 = openid + mobile（跨租户跟随同一微信用户/本人，独立于员工档案）。
-- 可见集合语义：请求员工可见公司 = {本登录公司} ∪ (本表 openid 匹配的 company_id)。
-- 无任何授权行 = 只自家公司（严格模式，与 mp_schedule_permission 一致）。
-- 越权边界：所有 /mp/dashboard 取数（含集团总表聚合）只限可见集合。
-- =====================================================
USE hrsystem;

CREATE TABLE IF NOT EXISTS `mp_employee_visible_company` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(64) NOT NULL COMMENT '员工微信openid（授权键）',
  `mobile` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '员工手机号（审计/跨库消歧）',
  `company_id` VARCHAR(10) NOT NULL COMMENT '被授权可见的公司ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid_company` (`openid`, `company_id`),
  KEY `idx_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序统计可见公司授权（openid+手机号, 公司级）';

-- 可选：按手机号检索历史授权（如需按姓名在PC端反查）
-- SELECT vc.* FROM mp_employee_visible_company vc
--   JOIN (SELECT DISTINCT mobile FROM mp_employee_visible_company WHERE mobile=? ) x USING (mobile);
