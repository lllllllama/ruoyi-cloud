-- T70: add the explicit submission withdrawal permission (MySQL 5.7, idempotent).
USE `ry-cloud`;

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '撤回成果提交', parent.menu_id, 2, '#', '', 1, 0,
       'F', '0', '0', 'task:submission:withdraw', '#',
       'admin', NOW(), '仅成果提交人可撤回待审核记录'
FROM (
    SELECT menu_id
    FROM sys_menu
    WHERE component = 'research/submission/index'
    ORDER BY menu_id
    LIMIT 1
) parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'task:submission:withdraw'
);

SET @withdraw_menu_id = (
    SELECT menu_id FROM sys_menu
    WHERE perms = 'task:submission:withdraw'
    ORDER BY menu_id LIMIT 1
);

-- Roles that can create or edit their own submissions also receive the endpoint permission.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT source_roles.role_id, @withdraw_menu_id
FROM (
    SELECT DISTINCT rm.role_id
    FROM sys_role_menu rm
    INNER JOIN sys_menu source_menu ON source_menu.menu_id = rm.menu_id
    WHERE source_menu.perms IN ('task:submission:add', 'task:submission:edit')
) source_roles
LEFT JOIN sys_role_menu existing
       ON existing.role_id = source_roles.role_id
      AND existing.menu_id = @withdraw_menu_id
WHERE @withdraw_menu_id IS NOT NULL
  AND existing.role_id IS NULL;
