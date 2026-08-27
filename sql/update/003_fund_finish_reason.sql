-- Add persisted finish reasons for allocation and use plans.
-- MySQL 5.7 compatible and safe to rerun; no existing table or data is removed.

DELIMITER $$

DROP PROCEDURE IF EXISTS `ry-fund`.`add_fund_finish_reason`$$
CREATE PROCEDURE `ry-fund`.`add_fund_finish_reason`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'ry-fund'
          AND TABLE_NAME = 'fund_allocation_plan'
          AND COLUMN_NAME = 'finish_reason'
    ) THEN
        ALTER TABLE `ry-fund`.`fund_allocation_plan`
            ADD COLUMN `finish_reason` varchar(500) DEFAULT NULL COMMENT '结束原因'
            AFTER `finish_type`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'ry-fund'
          AND TABLE_NAME = 'fund_use_plan'
          AND COLUMN_NAME = 'finish_reason'
    ) THEN
        ALTER TABLE `ry-fund`.`fund_use_plan`
            ADD COLUMN `finish_reason` varchar(500) DEFAULT NULL COMMENT '结束原因'
            AFTER `finish_type`;
    END IF;
END$$

CALL `ry-fund`.`add_fund_finish_reason`()$$
DROP PROCEDURE IF EXISTS `ry-fund`.`add_fund_finish_reason`$$

DELIMITER ;
