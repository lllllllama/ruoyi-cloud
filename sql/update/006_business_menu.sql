-- T56: research/task menus and fund menu alignment (MySQL 5.7, idempotent)
USE `ry-cloud`;

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课题管理', 0, 4, 'research-group', NULL, 1, 0,
       'M', '0', '0', '', 'education', 'admin', NOW(), '统一课题组基础管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'research-group');
SET @research_group_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'research-group' LIMIT 1);

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '课题管理', @research_group_menu_id, 1, 'group', 'research/group/index', 1, 0,
       'C', '0', '0', 'research:group:list', 'peoples', 'admin', NOW(), '课题、单位和成员管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @research_group_menu_id AND path = 'group');

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '任务调度', 0, 5, 'research', NULL, 1, 0,
       'M', '0', '0', '', 'tree-table', 'admin', NOW(), '课题任务调度'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'research');
SET @task_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'research' LIMIT 1);

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT v.menu_name, @task_menu_id, v.order_num, v.path, v.component, 1, 0,
       'C', v.visible, '0', v.perms, v.icon, 'admin', NOW(), v.remark
FROM (
    SELECT '年度任务' menu_name, 1 order_num, 'framework' path, 'research/framework/index' component, '0' visible, 'task:framework:list' perms, 'date' icon, '年度任务框架' remark
    UNION ALL SELECT '任务清单', 2, 'task', 'research/task/index', '0', 'task:info:list', 'tree', '三级任务树'
    UNION ALL SELECT '我的任务', 3, 'my-task', 'research/my-task/index', '0', 'task:submission:add', 'list', '当前用户可提交成果'
    UNION ALL SELECT '未审资料', 4, 'audit', 'research/audit/index', '0', 'task:submission:audit', 'validCode', '待审核成果资料'
    UNION ALL SELECT '归档资料', 5, 'archive', 'research/archive/index', '0', 'task:info:list', 'documentation', '已归档成果资料'
    UNION ALL SELECT '成果提交', 6, 'submission', 'research/submission/index', '1', 'task:submission:add', '#', '隐藏成果提交路由'
) v
LEFT JOIN sys_menu m ON m.parent_id = @task_menu_id AND m.path = v.path
WHERE m.menu_id IS NULL;

SET @fund_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'fund' LIMIT 1);
UPDATE sys_menu SET menu_name = '项目总资金', order_num = 1, update_by = 'admin', update_time = NOW()
WHERE parent_id = @fund_menu_id AND path = 'budget';
UPDATE sys_menu SET menu_name = '拨付管理', order_num = 2, update_by = 'admin', update_time = NOW()
WHERE parent_id = @fund_menu_id AND path = 'allocation';
UPDATE sys_menu SET menu_name = '使用管理', order_num = 3, update_by = 'admin', update_time = NOW()
WHERE parent_id = @fund_menu_id AND path = 'use';
UPDATE sys_menu SET order_num = 6, update_by = 'admin', update_time = NOW()
WHERE menu_id = @fund_menu_id;

-- The fund module no longer owns research topic CRUD; retain rows for auditability but disable them.
UPDATE sys_menu SET status = '1', visible = '1', update_by = 'admin', update_time = NOW()
WHERE parent_id = (SELECT menu_id FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @fund_menu_id AND path = 'budget' LIMIT 1) budget_menu)
  AND perms IN ('fund:topic:list', 'fund:topic:query', 'fund:topic:add', 'fund:topic:edit', 'fund:topic:remove');
