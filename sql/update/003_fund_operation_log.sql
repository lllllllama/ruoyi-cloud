-- Independent business audit log for Fund operations.
-- This complements sys_oper_log and does not use physical foreign keys.

CREATE TABLE IF NOT EXISTS `ry-fund`.`fund_operation_log` (
    `log_id` bigint NOT NULL AUTO_INCREMENT,
    `group_id` bigint NOT NULL,
    `business_type` varchar(32) NOT NULL,
    `business_id` bigint NOT NULL,
    `operation_type` varchar(32) NOT NULL,
    `before_data` longtext DEFAULT NULL,
    `after_data` longtext DEFAULT NULL,
    `reason` varchar(500) DEFAULT NULL,
    `operator_id` bigint NOT NULL,
    `operation_time` datetime NOT NULL,
    PRIMARY KEY (`log_id`),
    KEY `idx_fund_log_group_time` (`group_id`, `operation_time`),
    KEY `idx_fund_log_business` (`business_type`, `business_id`),
    KEY `idx_fund_log_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金业务审计日志';
