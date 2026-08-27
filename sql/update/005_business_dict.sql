-- T55: research, task and fund business dictionaries (MySQL 5.7, idempotent)
USE `ry-cloud`;

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '课题组成员角色', 'research_group_role', '0', 'admin', NOW(), '课题组成员业务身份'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'research_group_role');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '任务状态', 'task_status', '0', 'admin', NOW(), '任务业务状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'task_status');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '成果提交状态', 'task_submission_status', '0', 'admin', NOW(), '成果提交审核状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'task_submission_status');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '任务时间状态', 'task_time_status', '0', 'admin', NOW(), '运行时计算，不写入任务业务状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'task_time_status');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '资金计划状态', 'fund_plan_status', '0', 'admin', NOW(), '资金计划业务状态'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'fund_plan_status');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '资金执行结果', 'fund_execution_result', '0', 'admin', NOW(), '资金计划运行时执行结果'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'fund_execution_result');
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '资金结束类型', 'fund_finish_type', '0', 'admin', NOW(), '资金计划结束差异类型'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'fund_finish_type');

INSERT INTO sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
     is_default, status, create_by, create_time, remark)
SELECT v.dict_sort, v.dict_label, v.dict_value, v.dict_type, '', v.list_class,
       v.is_default, '0', 'admin', NOW(), v.remark
FROM (
    SELECT 1 dict_sort, '课题负责人' dict_label, 'LEADER' dict_value, 'research_group_role' dict_type, 'danger' list_class, 'N' is_default, '课题负责人' remark
    UNION ALL SELECT 2, '核心成员', 'CORE', 'research_group_role', 'warning', 'N', '核心成员'
    UNION ALL SELECT 3, '普通成员', 'MEMBER', 'research_group_role', 'primary', 'Y', '普通成员'
    UNION ALL SELECT 4, '专家', 'EXPERT', 'research_group_role', 'info', 'N', '专家成员'
    UNION ALL SELECT 1, '草稿', '0', 'task_status', 'info', 'Y', '任务草稿'
    UNION ALL SELECT 2, '进行中', '1', 'task_status', 'primary', 'N', '任务进行中'
    UNION ALL SELECT 3, '已完成', '2', 'task_status', 'success', 'N', '任务已完成'
    UNION ALL SELECT 4, '已关闭', '3', 'task_status', 'warning', 'N', '任务已关闭'
    UNION ALL SELECT 1, '草稿', '0', 'task_submission_status', 'info', 'Y', '提交草稿'
    UNION ALL SELECT 2, '待审核', '1', 'task_submission_status', 'warning', 'N', '等待负责人审核'
    UNION ALL SELECT 3, '已退回', '2', 'task_submission_status', 'danger', 'N', '审核退回'
    UNION ALL SELECT 4, '已归档', '3', 'task_submission_status', 'success', 'N', '审核通过归档'
    UNION ALL SELECT 1, '正常', 'NORMAL', 'task_time_status', 'info', 'Y', '未临近截止'
    UNION ALL SELECT 2, '临近截止', 'NEAR_DUE', 'task_time_status', 'warning', 'N', '七天内截止'
    UNION ALL SELECT 3, '已逾期', 'OVERDUE', 'task_time_status', 'danger', 'N', '超过截止日期'
    UNION ALL SELECT 1, '执行中', '0', 'fund_plan_status', 'primary', 'Y', '资金计划执行中'
    UNION ALL SELECT 2, '已结束', '1', 'fund_plan_status', 'success', 'N', '资金计划已结束'
    UNION ALL SELECT 1, '执行中', 'RUNNING', 'fund_execution_result', 'info', 'Y', '计划尚未结束'
    UNION ALL SELECT 2, '按计划完成', 'EXACT', 'fund_execution_result', 'success', 'N', '实际金额等于计划金额'
    UNION ALL SELECT 3, '低于计划', 'UNDER', 'fund_execution_result', 'warning', 'N', '实际金额低于计划金额'
    UNION ALL SELECT 4, '超出计划', 'OVER', 'fund_execution_result', 'danger', 'N', '实际金额超出计划金额'
    UNION ALL SELECT 5, '待负责人确认', 'PENDING_CONFIRM', 'fund_execution_result', 'warning', 'N', '超支强制结束待确认'
    UNION ALL SELECT 1, '按计划完成', '0', 'fund_finish_type', 'success', 'Y', '实际金额等于计划金额'
    UNION ALL SELECT 2, '低于计划', '1', 'fund_finish_type', 'warning', 'N', '实际金额低于计划金额'
    UNION ALL SELECT 3, '超出计划', '2', 'fund_finish_type', 'danger', 'N', '实际金额超出计划金额'
) v
LEFT JOIN sys_dict_data d ON d.dict_type = v.dict_type AND d.dict_value = v.dict_value
WHERE d.dict_code IS NULL;
