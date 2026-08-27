-- Fund-owned attachment relationships; physical files remain in ruoyi-file.
-- Legacy voucher_urls values are preserved and migrated without deleting source data.

CREATE TABLE IF NOT EXISTS `ry-fund`.`fund_attachment` (
    `attachment_id` bigint NOT NULL AUTO_INCREMENT,
    `group_id` bigint NOT NULL,
    `business_type` varchar(32) NOT NULL,
    `business_id` bigint NOT NULL,
    `file_name` varchar(255) NOT NULL,
    `original_name` varchar(255) NOT NULL,
    `file_url` varchar(1000) NOT NULL,
    `file_size` bigint DEFAULT NULL,
    `file_type` varchar(64) DEFAULT NULL,
    `upload_user_id` bigint NOT NULL,
    `upload_time` datetime NOT NULL,
    `del_flag` char(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`attachment_id`),
    KEY `idx_fund_attachment_group` (`group_id`),
    KEY `idx_fund_attachment_business` (`business_type`, `business_id`),
    KEY `idx_fund_attachment_uploader` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金业务附件';

INSERT INTO `ry-fund`.`fund_attachment`
    (group_id,business_type,business_id,file_name,original_name,file_url,
     file_size,file_type,upload_user_id,upload_time,del_flag)
SELECT DISTINCT source.group_id,
       'ALLOCATION_RECORD',
       source.business_id,
       LEFT(source.file_name, 255),
       LEFT(source.file_name, 255),
       source.file_url,
       NULL,
       IF(LOCATE('.', source.file_name) > 0, LEFT(LOWER(SUBSTRING_INDEX(source.file_name, '.', -1)), 64), NULL),
       source.upload_user_id,
       source.upload_time,
       '0'
FROM (
    SELECT p.topic_id AS group_id,
           r.record_id AS business_id,
           TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(r.voucher_urls, ',', numbers.n), ',', -1)) AS file_url,
           SUBSTRING_INDEX(
               SUBSTRING_INDEX(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(r.voucher_urls, ',', numbers.n), ',', -1)), '?', 1),
               '/', -1
           ) AS file_name,
           r.submit_user_id AS upload_user_id,
           COALESCE(r.create_time, NOW()) AS upload_time
    FROM `ry-fund`.`fund_allocation_record` r
    INNER JOIN `ry-fund`.`fund_allocation_plan` p
      ON p.plan_id = r.plan_id AND p.del_flag = '0'
    INNER JOIN (
        SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
        UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
        UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
        UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    ) numbers
      ON numbers.n <= 1 + LENGTH(r.voucher_urls) - LENGTH(REPLACE(r.voucher_urls, ',', ''))
    WHERE r.del_flag = '0' AND r.voucher_urls IS NOT NULL AND TRIM(r.voucher_urls) <> ''
) source
WHERE source.file_url <> ''
  AND NOT EXISTS (
      SELECT 1 FROM `ry-fund`.`fund_attachment` a
      WHERE a.business_type = 'ALLOCATION_RECORD'
        AND a.business_id = source.business_id
        AND a.file_url = source.file_url
        AND a.del_flag = '0'
  );

INSERT INTO `ry-fund`.`fund_attachment`
    (group_id,business_type,business_id,file_name,original_name,file_url,
     file_size,file_type,upload_user_id,upload_time,del_flag)
SELECT DISTINCT source.group_id,
       'USE_RECORD',
       source.business_id,
       LEFT(source.file_name, 255),
       LEFT(source.file_name, 255),
       source.file_url,
       NULL,
       IF(LOCATE('.', source.file_name) > 0, LEFT(LOWER(SUBSTRING_INDEX(source.file_name, '.', -1)), 64), NULL),
       source.upload_user_id,
       source.upload_time,
       '0'
FROM (
    SELECT p.topic_id AS group_id,
           r.use_record_id AS business_id,
           TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(r.voucher_urls, ',', numbers.n), ',', -1)) AS file_url,
           SUBSTRING_INDEX(
               SUBSTRING_INDEX(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(r.voucher_urls, ',', numbers.n), ',', -1)), '?', 1),
               '/', -1
           ) AS file_name,
           r.submit_user_id AS upload_user_id,
           COALESCE(r.create_time, NOW()) AS upload_time
    FROM `ry-fund`.`fund_use_record` r
    INNER JOIN `ry-fund`.`fund_use_plan` p
      ON p.use_plan_id = r.use_plan_id AND p.del_flag = '0'
    INNER JOIN (
        SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
        UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
        UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
        UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    ) numbers
      ON numbers.n <= 1 + LENGTH(r.voucher_urls) - LENGTH(REPLACE(r.voucher_urls, ',', ''))
    WHERE r.del_flag = '0' AND r.voucher_urls IS NOT NULL AND TRIM(r.voucher_urls) <> ''
) source
WHERE source.file_url <> ''
  AND NOT EXISTS (
      SELECT 1 FROM `ry-fund`.`fund_attachment` a
      WHERE a.business_type = 'USE_RECORD'
        AND a.business_id = source.business_id
        AND a.file_url = source.file_url
        AND a.del_flag = '0'
  );
