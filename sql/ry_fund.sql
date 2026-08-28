-- 资金管理微服务数据库（业务库）
CREATE DATABASE IF NOT EXISTS `ry-fund` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `ry-fund`;

CREATE TABLE IF NOT EXISTS fund_topic (
  topic_id bigint NOT NULL AUTO_INCREMENT,
  topic_name varchar(200) NOT NULL,
  lead_dept_id bigint NOT NULL,
  lead_dept_name varchar(100) NOT NULL,
  leader_user_id bigint NOT NULL,
  leader_user_name varchar(100) NOT NULL,
  status char(1) DEFAULT '0',
  remark varchar(500) DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY(topic_id), KEY idx_leader(leader_user_id), KEY idx_lead_dept(lead_dept_id)
) ENGINE=InnoDB COMMENT='资金模块课题引用/权限配置';

CREATE TABLE IF NOT EXISTS fund_topic_dept (
  id bigint NOT NULL AUTO_INCREMENT, topic_id bigint NOT NULL, dept_id bigint NOT NULL,
  dept_type char(1) NOT NULL COMMENT '0负责单位 1参与单位', dept_name varchar(100) NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_topic_dept(topic_id,dept_id), KEY idx_dept(dept_id)
) ENGINE=InnoDB COMMENT='课题参与单位';

CREATE TABLE IF NOT EXISTS fund_project_budget (
  budget_id bigint NOT NULL AUTO_INCREMENT, topic_id bigint NOT NULL,
  total_amount decimal(18,2) NOT NULL, plan_end_time datetime DEFAULT NULL, fund_desc varchar(500) DEFAULT NULL,
  status char(1) DEFAULT '0', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL, del_flag char(1) DEFAULT '0',
  PRIMARY KEY(budget_id), UNIQUE KEY uk_topic_budget(topic_id), CONSTRAINT chk_budget_amount CHECK (total_amount > 0)
) ENGINE=InnoDB COMMENT='课题总资金';

CREATE TABLE IF NOT EXISTS fund_allocation_plan (
  plan_id bigint NOT NULL AUTO_INCREMENT, budget_id bigint NOT NULL, topic_id bigint NOT NULL,
  allocation_name varchar(200) NOT NULL, allocation_dept_id bigint NOT NULL, allocation_dept_name varchar(100) NOT NULL,
  receive_dept_id bigint NOT NULL, receive_dept_name varchar(100) NOT NULL,
  plan_amount decimal(18,2) NOT NULL, plan_time datetime DEFAULT NULL, fund_desc varchar(500) DEFAULT NULL,
  responsible_user_id bigint DEFAULT NULL, responsible_user_name varchar(100) DEFAULT NULL,
  status char(1) DEFAULT '0', actual_amount decimal(18,2) DEFAULT NULL, difference_amount decimal(18,2) DEFAULT NULL,
  finish_type char(1) DEFAULT NULL, finish_user_id bigint DEFAULT NULL, finish_time datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL, update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL, del_flag char(1) DEFAULT '0',
  PRIMARY KEY(plan_id), KEY idx_topic(topic_id), KEY idx_budget(budget_id), KEY idx_allocation_dept(allocation_dept_id), KEY idx_responsible(responsible_user_id)
) ENGINE=InnoDB COMMENT='资金拨付计划';

CREATE TABLE IF NOT EXISTS fund_allocation_record (
  record_id bigint NOT NULL AUTO_INCREMENT, plan_id bigint NOT NULL, allocation_name varchar(200) NOT NULL,
  amount decimal(18,2) NOT NULL, allocation_time datetime NOT NULL, fund_desc varchar(500) DEFAULT NULL,
  voucher_urls varchar(2000) DEFAULT NULL, submit_user_id bigint NOT NULL, submit_user_name varchar(100) NOT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL, update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL, del_flag char(1) DEFAULT '0',
  PRIMARY KEY(record_id), KEY idx_plan(plan_id), KEY idx_submit_user(submit_user_id)
) ENGINE=InnoDB COMMENT='资金拨付记录';

CREATE TABLE IF NOT EXISTS fund_use_plan (
  use_plan_id bigint NOT NULL AUTO_INCREMENT, budget_id bigint NOT NULL, topic_id bigint NOT NULL,
  use_name varchar(200) NOT NULL, plan_amount decimal(18,2) NOT NULL,
  responsible_user_id bigint DEFAULT NULL, responsible_user_name varchar(100) DEFAULT NULL,
  plan_time datetime DEFAULT NULL, fund_desc varchar(500) DEFAULT NULL,
  status char(1) DEFAULT '0', actual_amount decimal(18,2) DEFAULT NULL, difference_amount decimal(18,2) DEFAULT NULL,
  finish_type char(1) DEFAULT NULL, finish_user_id bigint DEFAULT NULL, finish_time datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL, update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL, del_flag char(1) DEFAULT '0',
  PRIMARY KEY(use_plan_id), KEY idx_topic(topic_id), KEY idx_budget(budget_id), KEY idx_responsible(responsible_user_id)
) ENGINE=InnoDB COMMENT='资金使用计划';

CREATE TABLE IF NOT EXISTS fund_use_record (
  use_record_id bigint NOT NULL AUTO_INCREMENT, use_plan_id bigint NOT NULL, use_name varchar(200) NOT NULL,
  amount decimal(18,2) NOT NULL, use_time datetime NOT NULL, fund_desc varchar(500) DEFAULT NULL,
  voucher_urls varchar(2000) DEFAULT NULL, submit_user_id bigint NOT NULL, submit_user_name varchar(100) NOT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL, update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL, del_flag char(1) DEFAULT '0',
  PRIMARY KEY(use_record_id), KEY idx_plan(use_plan_id), KEY idx_submit_user(submit_user_id)
) ENGINE=InnoDB COMMENT='资金使用记录';

CREATE TABLE IF NOT EXISTS fund_attachment (
  attachment_id bigint NOT NULL AUTO_INCREMENT, group_id bigint NOT NULL,
  business_type varchar(32) NOT NULL, business_id bigint NOT NULL,
  file_name varchar(255) NOT NULL, original_name varchar(255) NOT NULL,
  file_url varchar(1000) NOT NULL, file_size bigint DEFAULT NULL, file_type varchar(64) DEFAULT NULL,
  upload_user_id bigint NOT NULL, upload_time datetime NOT NULL, del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY(attachment_id), KEY idx_fund_attachment_group(group_id),
  KEY idx_fund_attachment_business(business_type,business_id), KEY idx_fund_attachment_uploader(upload_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金业务附件';

CREATE TABLE IF NOT EXISTS fund_upload_receipt (
  upload_token char(32) NOT NULL, file_name varchar(255) NOT NULL, original_name varchar(255) NOT NULL,
  file_url varchar(1000) NOT NULL, file_size bigint NOT NULL, file_type varchar(64) NOT NULL,
  upload_user_id bigint NOT NULL, upload_time datetime NOT NULL, expire_time datetime NOT NULL,
  used_flag char(1) NOT NULL DEFAULT '0', used_time datetime DEFAULT NULL,
  business_type varchar(32) DEFAULT NULL, business_id bigint DEFAULT NULL,
  PRIMARY KEY(upload_token), KEY idx_fund_upload_user(upload_user_id,used_flag),
  KEY idx_fund_upload_expire(expire_time,used_flag), KEY idx_fund_upload_business(business_type,business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金附件一次性上传凭证';

-- 菜单与按钮权限安装到系统库。采用动态ID，避免与已上线系统的自定义菜单冲突。
USE `ry-cloud`;

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,query,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT '资金管理',0,4,'fund',NULL,'',1,0,'M','0','0','','money','admin',sysdate(),'',NULL,'资金管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=0 AND path='fund');
SET @fund_menu_id=(SELECT menu_id FROM sys_menu WHERE parent_id=0 AND path='fund' ORDER BY menu_id LIMIT 1);

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,query,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT '资金总览',@fund_menu_id,1,'budget','fund/budget/index','',1,0,'C','0','0','fund:budget:list','dashboard','admin',sysdate(),'',NULL,'课题与总资金'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@fund_menu_id AND path='budget');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,query,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT '拨付管理',@fund_menu_id,2,'allocation','fund/allocation/index','',1,0,'C','0','0','fund:allocation:list','money','admin',sysdate(),'',NULL,'资金拨付'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@fund_menu_id AND path='allocation');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,query,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT '使用管理',@fund_menu_id,3,'use','fund/use/index','',1,0,'C','0','0','fund:use:list','form','admin',sysdate(),'',NULL,'资金使用'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@fund_menu_id AND path='use');
SET @budget_menu_id=(SELECT menu_id FROM sys_menu WHERE parent_id=@fund_menu_id AND path='budget' ORDER BY menu_id LIMIT 1);
SET @allocation_menu_id=(SELECT menu_id FROM sys_menu WHERE parent_id=@fund_menu_id AND path='allocation' ORDER BY menu_id LIMIT 1);
SET @use_menu_id=(SELECT menu_id FROM sys_menu WHERE parent_id=@fund_menu_id AND path='use' ORDER BY menu_id LIMIT 1);

-- 按钮权限：以 perms 判重。
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,query,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT x.menu_name,x.parent_id,x.order_num,'#','','',1,0,'F','0','0',x.perms,'#','admin',sysdate(),'',NULL,''
FROM (
  SELECT '课题查询' menu_name,@budget_menu_id parent_id,1 order_num,'fund:topic:list' perms UNION ALL
  SELECT '课题详情',@budget_menu_id,2,'fund:topic:query' UNION ALL
  SELECT '课题新增',@budget_menu_id,3,'fund:topic:add' UNION ALL
  SELECT '课题修改',@budget_menu_id,4,'fund:topic:edit' UNION ALL
  SELECT '课题删除',@budget_menu_id,5,'fund:topic:remove' UNION ALL
  SELECT '总资金新增',@budget_menu_id,6,'fund:budget:add' UNION ALL
  SELECT '总资金修改',@budget_menu_id,7,'fund:budget:edit' UNION ALL
  SELECT '总资金删除',@budget_menu_id,8,'fund:budget:remove' UNION ALL
  SELECT '拨付新增',@allocation_menu_id,1,'fund:allocation:add' UNION ALL
  SELECT '拨付修改',@allocation_menu_id,2,'fund:allocation:edit' UNION ALL
  SELECT '拨付删除',@allocation_menu_id,3,'fund:allocation:remove' UNION ALL
  SELECT '指定责任人',@allocation_menu_id,4,'fund:allocation:assign' UNION ALL
  SELECT '拨付记录',@allocation_menu_id,5,'fund:allocation:record' UNION ALL
  SELECT '结束拨付',@allocation_menu_id,6,'fund:allocation:finish' UNION ALL
  SELECT '使用计划新增',@use_menu_id,1,'fund:use:add' UNION ALL
  SELECT '使用计划修改',@use_menu_id,2,'fund:use:edit' UNION ALL
  SELECT '使用计划删除',@use_menu_id,3,'fund:use:remove' UNION ALL
  SELECT '使用记录',@use_menu_id,4,'fund:use:record' UNION ALL
  SELECT '结束使用计划',@use_menu_id,5,'fund:use:finish'
) x
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.perms=x.perms);

-- 所有登录用户可进入拨付页查看公开汇总；计划、记录和附件由后端按课题成员关系校验。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,@fund_menu_id FROM sys_role WHERE status='0' AND del_flag='0';
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,@allocation_menu_id FROM sys_role WHERE status='0' AND del_flag='0';
-- 使用管理菜单也挂给现有角色，真正的数据可见性由 fund-service 按课题成员关系二次校验。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT role_id,@use_menu_id FROM sys_role WHERE status='0' AND del_flag='0';
