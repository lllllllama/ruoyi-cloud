-- Research service database baseline.
-- MySQL 5.7 compatible; relationships are enforced by services, not physical foreign keys.

CREATE DATABASE IF NOT EXISTS `ry-research`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `ry-research`;

CREATE TABLE IF NOT EXISTS `biz_research_group` (
  `group_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Research group ID',
  `group_code` varchar(64) NOT NULL COMMENT 'Unique group code',
  `group_name` varchar(200) NOT NULL COMMENT 'Group name',
  `lead_dept_id` bigint NOT NULL COMMENT 'Lead department ID',
  `description` varchar(1000) DEFAULT NULL COMMENT 'Description',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 active, 1 disabled',
  `sort` int NOT NULL DEFAULT 0 COMMENT 'Display order',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL COMMENT 'Updated time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'Logical delete flag',
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `uk_research_group_code` (`group_code`),
  KEY `idx_research_group_lead_dept` (`lead_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Research group';

CREATE TABLE IF NOT EXISTS `biz_research_group_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Relation ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `dept_id` bigint NOT NULL COMMENT 'Department ID',
  `unit_type` varchar(16) NOT NULL COMMENT 'LEAD or PARTICIPANT',
  `manager_user_id` bigint DEFAULT NULL COMMENT 'Unit manager user ID',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 active, 1 disabled',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_research_group_unit` (`group_id`, `dept_id`),
  KEY `idx_research_unit_group` (`group_id`),
  KEY `idx_research_unit_dept` (`dept_id`),
  KEY `idx_research_unit_manager` (`manager_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Research group unit';

CREATE TABLE IF NOT EXISTS `biz_research_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Membership ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `dept_id` bigint NOT NULL COMMENT 'Department ID at assignment time',
  `member_role` varchar(16) NOT NULL COMMENT 'LEADER, CORE, MEMBER or EXPERT',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 active, 1 disabled',
  `join_time` datetime DEFAULT NULL COMMENT 'Join time',
  `leave_time` datetime DEFAULT NULL COMMENT 'Leave time',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  `update_time` datetime DEFAULT NULL COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_research_group_member_role` (`group_id`, `user_id`, `member_role`),
  KEY `idx_research_member_group` (`group_id`),
  KEY `idx_research_member_user` (`user_id`),
  KEY `idx_research_member_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Research group member';
