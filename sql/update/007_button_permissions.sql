-- T57: business button permissions (MySQL 5.7, idempotent)
USE `ry-cloud`;

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT v.menu_name,
       (SELECT p.menu_id FROM sys_menu p WHERE p.component = v.parent_component LIMIT 1),
       v.order_num, '#', '', 1, 0, 'F', '0', '0', v.perms, '#',
       'admin', NOW(), '业务按钮权限'
FROM (
    SELECT '课题新增' menu_name, 'research/group/index' parent_component, 1 order_num, 'research:group:add' perms
    UNION ALL SELECT '课题修改', 'research/group/index', 2, 'research:group:edit'
    UNION ALL SELECT '年度任务新增', 'research/framework/index', 1, 'task:framework:add'
    UNION ALL SELECT '任务新增', 'research/task/index', 1, 'task:info:add'
    UNION ALL SELECT '任务修改', 'research/task/index', 2, 'task:info:edit'
    UNION ALL SELECT '任务删除', 'research/task/index', 3, 'task:info:remove'
    UNION ALL SELECT '成果新增修改', 'research/task/index', 4, 'task:deliverable:add'
    UNION ALL SELECT '成果责任人', 'research/task/index', 5, 'task:deliverable:assign'
    UNION ALL SELECT '成果草稿修改', 'research/submission/index', 1, 'task:submission:edit'
    UNION ALL SELECT '成果审核', 'research/audit/index', 1, 'task:submission:audit'
    UNION ALL SELECT '取消成果审核', 'research/archive/index', 1, 'task:submission:cancelAudit'
) v
LEFT JOIN sys_menu existing ON existing.perms = v.perms
WHERE existing.menu_id IS NULL
  AND (SELECT COUNT(*) FROM sys_menu p WHERE p.component = v.parent_component) > 0;

UPDATE sys_menu SET menu_name = '拨付记录', perms = 'fund:allocation:record',
       update_by = 'admin', update_time = NOW()
WHERE perms = 'fund:allocation:submit';
UPDATE sys_menu SET menu_name = '使用记录', perms = 'fund:use:record',
       update_by = 'admin', update_time = NOW()
WHERE perms = 'fund:use:submit';
