-- 为 admin_message 表新增 link_url 字段，用于存储通知跳转链接
-- 场景：进度条功能完成后，通知中心显示"前去查看"链接，点击跳转到对应功能页面
ALTER TABLE `admin_message` ADD COLUMN `link_url` varchar(255) DEFAULT NULL COMMENT '跳转链接';
