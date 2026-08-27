-- Safely migrate the legacy Fund topic model into the Research service database.
-- Prerequisite: sql/update/001_research_base.sql has been applied.
-- The legacy fund_topic and fund_topic_dept tables are intentionally retained.

DELIMITER $$

DROP PROCEDURE IF EXISTS `ry-research`.`migrate_fund_topics_to_research`$$
CREATE PROCEDURE `ry-research`.`migrate_fund_topics_to_research`()
BEGIN
    -- A reused primary key is safe only when it is already the same generated migration record.
    IF EXISTS (
        SELECT 1
        FROM `ry-fund`.`fund_topic` f
        INNER JOIN `ry-research`.`biz_research_group` g ON g.group_id = f.topic_id
        WHERE g.group_code <> CONCAT('FUND_TOPIC_', f.topic_id)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Fund topic migration stopped: target group_id collision';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM `ry-fund`.`fund_topic` f
        INNER JOIN `ry-research`.`biz_research_group` g
          ON g.group_code = CONCAT('FUND_TOPIC_', f.topic_id)
         AND g.group_id <> f.topic_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Fund topic migration stopped: generated group_code collision';
    END IF;

    INSERT INTO `ry-research`.`biz_research_group`
        (group_id, group_code, group_name, lead_dept_id, description, status, sort,
         create_by, create_time, update_by, update_time, remark, del_flag)
    SELECT f.topic_id,
           CONCAT('FUND_TOPIC_', f.topic_id),
           f.topic_name,
           f.lead_dept_id,
           NULL,
           IFNULL(f.status, '0'),
           0,
           IFNULL(f.create_by, ''),
           f.create_time,
           IFNULL(f.update_by, ''),
           f.update_time,
           f.remark,
           IFNULL(f.del_flag, '0')
    FROM `ry-fund`.`fund_topic` f
    ON DUPLICATE KEY UPDATE
        group_name = VALUES(group_name),
        lead_dept_id = VALUES(lead_dept_id),
        status = VALUES(status),
        update_by = VALUES(update_by),
        update_time = VALUES(update_time),
        remark = VALUES(remark),
        del_flag = VALUES(del_flag);

    -- Always derive one LEAD unit from fund_topic. LEAD takes precedence if legacy rows conflict.
    INSERT INTO `ry-research`.`biz_research_group_unit`
        (group_id, dept_id, unit_type, manager_user_id, status, create_by, create_time)
    SELECT source.group_id,
           source.dept_id,
           IF(MAX(source.is_lead) = 1, 'LEAD', 'PARTICIPANT'),
           NULL,
           '0',
           MAX(source.create_by),
           MIN(source.create_time)
    FROM (
        SELECT f.topic_id AS group_id,
               f.lead_dept_id AS dept_id,
               1 AS is_lead,
               IFNULL(f.create_by, '') AS create_by,
               f.create_time
        FROM `ry-fund`.`fund_topic` f
        UNION ALL
        SELECT d.topic_id AS group_id,
               d.dept_id,
               IF(d.dept_type = '0', 1, 0) AS is_lead,
               '' AS create_by,
               NULL AS create_time
        FROM `ry-fund`.`fund_topic_dept` d
        INNER JOIN `ry-fund`.`fund_topic` f ON f.topic_id = d.topic_id
    ) source
    GROUP BY source.group_id, source.dept_id
    ON DUPLICATE KEY UPDATE
        unit_type = VALUES(unit_type),
        status = VALUES(status);

    -- Preserve the legacy topic leader as a group member instead of losing the responsibility relation.
    INSERT INTO `ry-research`.`biz_research_group_member`
        (group_id, user_id, dept_id, member_role, status, join_time,
         create_by, create_time, update_time)
    SELECT f.topic_id,
           f.leader_user_id,
           f.lead_dept_id,
           'LEADER',
           '0',
           f.create_time,
           IFNULL(f.create_by, ''),
           f.create_time,
           f.update_time
    FROM `ry-fund`.`fund_topic` f
    WHERE f.leader_user_id IS NOT NULL
    ON DUPLICATE KEY UPDATE
        dept_id = VALUES(dept_id),
        status = VALUES(status),
        update_time = VALUES(update_time);
END$$

CALL `ry-research`.`migrate_fund_topics_to_research`()$$
DROP PROCEDURE IF EXISTS `ry-research`.`migrate_fund_topics_to_research`$$

DELIMITER ;
