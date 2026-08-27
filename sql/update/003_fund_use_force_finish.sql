-- Add the two-stage force-finish state for overspent use plans.
-- Prerequisite: 003_fund_finish_reason.sql.
-- MySQL 5.7 compatible and safe to rerun.

DELIMITER $$

DROP PROCEDURE IF EXISTS `ry-fund`.`add_fund_use_force_finish`$$
CREATE PROCEDURE `ry-fund`.`add_fund_use_force_finish`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'ry-fund'
          AND TABLE_NAME = 'fund_use_plan'
          AND COLUMN_NAME = 'force_finish'
    ) THEN
        ALTER TABLE `ry-fund`.`fund_use_plan`
            ADD COLUMN `force_finish` char(1) NOT NULL DEFAULT '0' COMMENT '是否强制结束'
            AFTER `finish_reason`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'ry-fund'
          AND TABLE_NAME = 'fund_use_plan'
          AND COLUMN_NAME = 'confirm_user_id'
    ) THEN
        ALTER TABLE `ry-fund`.`fund_use_plan`
            ADD COLUMN `confirm_user_id` bigint DEFAULT NULL COMMENT '强制结束确认人'
            AFTER `finish_time`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'ry-fund'
          AND TABLE_NAME = 'fund_use_plan'
          AND COLUMN_NAME = 'confirm_time'
    ) THEN
        ALTER TABLE `ry-fund`.`fund_use_plan`
            ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT '强制结束确认时间'
            AFTER `confirm_user_id`;
    END IF;
END$$

CALL `ry-fund`.`add_fund_use_force_finish`()$$
DROP PROCEDURE IF EXISTS `ry-fund`.`add_fund_use_force_finish`$$

DELIMITER ;
