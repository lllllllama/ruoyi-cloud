-- Repeatable TEST-only fixtures for the multi-role QA matrix.
-- Target: local MySQL 5.7 on 127.0.0.1:3307.
-- The reserved IDs below are intentionally isolated from normal demo data.

SET NAMES utf8mb4;
USE `ry-research`;

SET @group_a = 991000001;
SET @group_b = 991000002;

-- Remove only business data owned by the reserved QA groups.
DELETE a FROM `ry-research`.`task_attachment` a
INNER JOIN `ry-research`.`task_submission` s ON s.submission_id = a.submission_id
WHERE s.group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`task_submission_audit`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`task_submission`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`task_deliverable_user`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`task_deliverable`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`task_info`
WHERE group_id IN (@group_a, @group_b);
DELETE u FROM `ry-research`.`task_framework_unit` u
INNER JOIN `ry-research`.`task_framework` f ON f.framework_id = u.framework_id
WHERE f.group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`task_framework`
WHERE group_id IN (@group_a, @group_b);

DELETE FROM `ry-fund`.`fund_attachment`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-fund`.`fund_operation_log`
WHERE group_id IN (@group_a, @group_b);
DELETE r FROM `ry-fund`.`fund_allocation_record` r
INNER JOIN `ry-fund`.`fund_allocation_plan` p ON p.plan_id = r.plan_id
WHERE p.topic_id IN (@group_a, @group_b);
DELETE r FROM `ry-fund`.`fund_use_record` r
INNER JOIN `ry-fund`.`fund_use_plan` p ON p.use_plan_id = r.use_plan_id
WHERE p.topic_id IN (@group_a, @group_b);
DELETE FROM `ry-fund`.`fund_allocation_plan`
WHERE topic_id IN (@group_a, @group_b);
DELETE FROM `ry-fund`.`fund_use_plan`
WHERE topic_id IN (@group_a, @group_b);
DELETE FROM `ry-fund`.`fund_project_budget`
WHERE topic_id IN (@group_a, @group_b);

DELETE FROM `ry-research`.`biz_research_group_member`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`biz_research_group_unit`
WHERE group_id IN (@group_a, @group_b);
DELETE FROM `ry-research`.`biz_research_group`
WHERE group_id IN (@group_a, @group_b);

-- Fixed system roles. Business membership still comes from ry-research.
INSERT INTO `ry-cloud`.`sys_role`
    (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
    (9201, 'QA课题负责人', 'qa_research_leader', 91, '1', 1, 1, '0', '0', 'qa', NOW(), 'TEST only'),
    (9202, 'QA课题成员', 'qa_research_member', 92, '1', 1, 1, '0', '0', 'qa', NOW(), 'TEST only'),
    (9203, 'QA无关用户', 'qa_outsider', 93, '1', 1, 1, '0', '0', 'qa', NOW(), 'TEST only'),
    (9204, 'QA拨付单位负责人', 'qa_allocation_manager', 94, '1', 1, 1, '0', '0', 'qa', NOW(), 'TEST only'),
    (9205, 'QA拨付操作员', 'qa_allocation_operator', 95, '1', 1, 1, '0', '0', 'qa', NOW(), 'TEST only')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name), role_key = VALUES(role_key),
    role_sort = VALUES(role_sort), status = '0', del_flag = '0',
    update_by = 'qa', update_time = NOW(), remark = 'TEST only';

SET @qa_password = (
    SELECT password FROM `ry-cloud`.`sys_user` WHERE user_name = 'admin' LIMIT 1
);

INSERT INTO `ry-cloud`.`sys_user`
    (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber,
     sex, avatar, password, status, del_flag, create_by, create_time, remark)
VALUES
    (9101, 103, 'a_leader', 'A课题负责人', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9102, 103, 'a_core', 'A课题骨干', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9103, 104, 'a_member', 'A课题成员', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9104, 104, 'a_expert', 'A课题专家', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9105, 105, 'b_leader', 'B课题负责人', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9106, 105, 'b_core', 'B课题骨干', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9107, 106, 'outsider', '无关用户', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9108, 104, 'alloc_manager', '拨付单位负责人', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9109, 104, 'alloc_user', '拨付责任人', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only'),
    (9110, 104, 'other_unit_user', '参与单位普通成员', '00', '', '', '0', '', @qa_password, '0', '0', 'qa', NOW(), 'TEST only')
ON DUPLICATE KEY UPDATE
    dept_id = VALUES(dept_id), user_name = VALUES(user_name),
    nick_name = VALUES(nick_name), password = VALUES(password),
    status = '0', del_flag = '0', update_by = 'qa', update_time = NOW(),
    remark = 'TEST only';

DELETE FROM `ry-cloud`.`sys_user_role`
WHERE user_id BETWEEN 9101 AND 9110;
INSERT INTO `ry-cloud`.`sys_user_role` (user_id, role_id) VALUES
    (9101, 9201), (9102, 9202), (9103, 9202), (9104, 9202),
    (9105, 9201), (9106, 9202), (9107, 9203), (9108, 9204),
    (9109, 9205), (9110, 9205);

DELETE FROM `ry-cloud`.`sys_role_menu`
WHERE role_id BETWEEN 9201 AND 9205;

-- Leader: maintain task structures, audit, and manage this group's use plans.
INSERT IGNORE INTO `ry-cloud`.`sys_role_menu` (role_id, menu_id)
SELECT 9201, menu_id FROM `ry-cloud`.`sys_menu`
WHERE path IN ('fund', 'allocation', 'use', 'research', 'framework', 'task',
               'my-task', 'audit', 'archive', 'submission')
   OR perms IN (
       'fund:allocation:list', 'fund:allocation:record', 'fund:allocation:finish',
       'fund:use:list', 'fund:use:add',
       'fund:use:edit', 'fund:use:remove', 'fund:use:record', 'fund:use:finish',
       'task:framework:list', 'task:framework:add', 'task:info:list',
       'task:info:add', 'task:info:edit', 'task:info:remove',
       'task:deliverable:add', 'task:deliverable:assign',
       'task:submission:add', 'task:submission:edit', 'task:submission:withdraw',
       'task:submission:audit', 'task:submission:cancelAudit'
   );

-- Core/member/expert share UI permissions; service rules distinguish their group role.
INSERT IGNORE INTO `ry-cloud`.`sys_role_menu` (role_id, menu_id)
SELECT 9202, menu_id FROM `ry-cloud`.`sys_menu`
WHERE path IN ('fund', 'allocation', 'use', 'research', 'task',
               'my-task', 'archive', 'submission')
   OR perms IN ('fund:allocation:list', 'fund:use:list', 'fund:use:record', 'fund:use:finish',
                'task:info:list', 'task:submission:add', 'task:submission:edit',
                'task:submission:withdraw');

-- Outsider is intentionally granted only the public allocation page.
INSERT IGNORE INTO `ry-cloud`.`sys_role_menu` (role_id, menu_id)
SELECT 9203, menu_id FROM `ry-cloud`.`sys_menu`
WHERE path IN ('fund', 'allocation') OR perms = 'fund:allocation:list';

-- Allocation unit manager can assign and record; operators can only record.
INSERT IGNORE INTO `ry-cloud`.`sys_role_menu` (role_id, menu_id)
SELECT 9204, menu_id FROM `ry-cloud`.`sys_menu`
WHERE path IN ('fund', 'allocation')
   OR perms IN ('fund:allocation:list', 'fund:allocation:assign',
                'fund:allocation:record', 'fund:allocation:finish');
INSERT IGNORE INTO `ry-cloud`.`sys_role_menu` (role_id, menu_id)
SELECT 9205, menu_id FROM `ry-cloud`.`sys_menu`
WHERE path IN ('fund', 'allocation')
   OR perms IN ('fund:allocation:list', 'fund:allocation:record', 'fund:allocation:finish');

-- Fixed A/B research-group model.
INSERT INTO `ry-research`.`biz_research_group`
    (group_id, group_code, group_name, lead_dept_id, description, status, sort,
     create_by, create_time, remark, del_flag)
VALUES
    (@group_a, 'QA-GROUP-A', 'QA课题A', 103, 'Multi-role integration test group A', '0', 1, 'qa', NOW(), 'TEST only', '0'),
    (@group_b, 'QA-GROUP-B', 'QA课题B', 105, 'Cross-group isolation test group B', '0', 2, 'qa', NOW(), 'TEST only', '0');

INSERT INTO `ry-research`.`biz_research_group_unit`
    (group_id, dept_id, unit_type, manager_user_id, status, create_by, create_time)
VALUES
    (@group_a, 103, 'LEAD', 9101, '0', 'qa', NOW()),
    (@group_a, 104, 'PARTICIPANT', 9108, '0', 'qa', NOW()),
    (@group_b, 105, 'LEAD', 9105, '0', 'qa', NOW());

INSERT INTO `ry-research`.`biz_research_group_member`
    (group_id, user_id, dept_id, member_role, status, join_time, create_by, create_time)
VALUES
    (@group_a, 9101, 103, 'LEADER', '0', NOW(), 'qa', NOW()),
    (@group_a, 9102, 103, 'CORE', '0', NOW(), 'qa', NOW()),
    (@group_a, 9103, 104, 'MEMBER', '0', NOW(), 'qa', NOW()),
    (@group_a, 9104, 104, 'EXPERT', '0', NOW(), 'qa', NOW()),
    (@group_a, 9108, 104, 'MEMBER', '0', NOW(), 'qa', NOW()),
    (@group_a, 9109, 104, 'MEMBER', '0', NOW(), 'qa', NOW()),
    (@group_a, 9110, 104, 'MEMBER', '0', NOW(), 'qa', NOW()),
    (@group_b, 9105, 105, 'LEADER', '0', NOW(), 'qa', NOW()),
    (@group_b, 9106, 105, 'CORE', '0', NOW(), 'qa', NOW());

SELECT 'QA_FIXTURES_READY' AS status, @group_a AS group_a, @group_b AS group_b;
