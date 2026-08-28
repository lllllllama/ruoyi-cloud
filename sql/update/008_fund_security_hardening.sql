-- Fund permission and attachment upload hardening (MySQL 5.7, idempotent).

CREATE TABLE IF NOT EXISTS `ry-fund`.`fund_upload_receipt` (
    `upload_token` char(32) NOT NULL,
    `file_name` varchar(255) NOT NULL,
    `original_name` varchar(255) NOT NULL,
    `file_url` varchar(1000) NOT NULL,
    `file_size` bigint NOT NULL,
    `file_type` varchar(64) NOT NULL,
    `upload_user_id` bigint NOT NULL,
    `upload_time` datetime NOT NULL,
    `expire_time` datetime NOT NULL,
    `used_flag` char(1) NOT NULL DEFAULT '0',
    `used_time` datetime DEFAULT NULL,
    `business_type` varchar(32) DEFAULT NULL,
    `business_id` bigint DEFAULT NULL,
    PRIMARY KEY (`upload_token`),
    KEY `idx_fund_upload_user` (`upload_user_id`, `used_flag`),
    KEY `idx_fund_upload_expire` (`expire_time`, `used_flag`),
    KEY `idx_fund_upload_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金附件一次性上传凭证';

USE `ry-cloud`;

UPDATE sys_menu SET menu_name = '拨付记录', perms = 'fund:allocation:record',
       update_by = 'admin', update_time = NOW()
WHERE perms = 'fund:allocation:submit';

UPDATE sys_menu SET menu_name = '使用记录', perms = 'fund:use:record',
       update_by = 'admin', update_time = NOW()
WHERE perms = 'fund:use:submit';
