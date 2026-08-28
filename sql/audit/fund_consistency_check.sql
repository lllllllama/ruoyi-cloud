-- Fund historical consistency audit (read-only, MySQL 5.7 compatible).
-- Expected result: zero rows. Any returned row must be reviewed manually.
-- This script never inserts, updates, deletes, or repairs business data.

SELECT
    totals.topic_id AS topicId,
    totals.topic_name AS topicName,
    totals.total_budget AS totalBudget,
    totals.planned_allocation AS plannedAllocation,
    totals.actual_allocation AS actualAllocation,
    totals.planned_use AS plannedUse,
    totals.actual_use AS actualUse,
    anomaly.anomaly_type AS anomalyType,
    CASE anomaly.anomaly_code
        WHEN 'A' THEN totals.planned_allocation - totals.total_budget
        WHEN 'B' THEN totals.planned_use - totals.total_budget
        WHEN 'C' THEN totals.actual_allocation - totals.total_budget
        WHEN 'D' THEN totals.actual_use - totals.total_budget
        WHEN 'E' THEN totals.actual_use - totals.actual_allocation
    END AS anomalyDifference
FROM
(
    SELECT
        budget.topic_id,
        COALESCE(research_group.group_name, legacy_topic.topic_name,
                 CONCAT('topic#', budget.topic_id)) AS topic_name,
        budget.total_amount AS total_budget,
        COALESCE(allocation_plan.planned_amount, 0.00) AS planned_allocation,
        COALESCE(allocation_record.actual_amount, 0.00) AS actual_allocation,
        COALESCE(use_plan.planned_amount, 0.00) AS planned_use,
        COALESCE(use_record.actual_amount, 0.00) AS actual_use
    FROM `ry-fund`.fund_project_budget budget
    LEFT JOIN `ry-research`.biz_research_group research_group
        ON research_group.group_id = budget.topic_id
       AND research_group.del_flag = '0'
    LEFT JOIN `ry-fund`.fund_topic legacy_topic
        ON legacy_topic.topic_id = budget.topic_id
       AND legacy_topic.del_flag = '0'
    LEFT JOIN
    (
        SELECT topic_id, SUM(plan_amount) AS planned_amount
        FROM `ry-fund`.fund_allocation_plan
        WHERE del_flag = '0'
        GROUP BY topic_id
    ) allocation_plan
        ON allocation_plan.topic_id = budget.topic_id
    LEFT JOIN
    (
        SELECT plan.topic_id, SUM(record.amount) AS actual_amount
        FROM `ry-fund`.fund_allocation_plan plan
        INNER JOIN `ry-fund`.fund_allocation_record record
            ON record.plan_id = plan.plan_id
           AND record.del_flag = '0'
        WHERE plan.del_flag = '0'
        GROUP BY plan.topic_id
    ) allocation_record
        ON allocation_record.topic_id = budget.topic_id
    LEFT JOIN
    (
        SELECT topic_id, SUM(plan_amount) AS planned_amount
        FROM `ry-fund`.fund_use_plan
        WHERE del_flag = '0'
        GROUP BY topic_id
    ) use_plan
        ON use_plan.topic_id = budget.topic_id
    LEFT JOIN
    (
        SELECT plan.topic_id, SUM(record.amount) AS actual_amount
        FROM `ry-fund`.fund_use_plan plan
        INNER JOIN `ry-fund`.fund_use_record record
            ON record.use_plan_id = plan.use_plan_id
           AND record.del_flag = '0'
        WHERE plan.del_flag = '0'
        GROUP BY plan.topic_id
    ) use_record
        ON use_record.topic_id = budget.topic_id
    WHERE budget.del_flag = '0'
) totals
INNER JOIN
(
    SELECT 'A' AS anomaly_code, 'PLANNED_ALLOCATION_OVER_BUDGET' AS anomaly_type
    UNION ALL SELECT 'B', 'PLANNED_USE_OVER_BUDGET'
    UNION ALL SELECT 'C', 'ACTUAL_ALLOCATION_OVER_BUDGET'
    UNION ALL SELECT 'D', 'ACTUAL_USE_OVER_BUDGET'
    UNION ALL SELECT 'E', 'ACTUAL_USE_OVER_ACTUAL_ALLOCATION'
) anomaly
    ON CASE anomaly.anomaly_code
        WHEN 'A' THEN totals.planned_allocation > totals.total_budget
        WHEN 'B' THEN totals.planned_use > totals.total_budget
        WHEN 'C' THEN totals.actual_allocation > totals.total_budget
        WHEN 'D' THEN totals.actual_use > totals.total_budget
        WHEN 'E' THEN totals.actual_use > totals.actual_allocation
    END
ORDER BY totals.topic_id, anomaly.anomaly_code;
