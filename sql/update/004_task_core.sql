-- Research task scheduling core tables.
-- MySQL 5.7 compatible; all cross-table relationships are logical and indexed.

CREATE DATABASE IF NOT EXISTS `ry-research`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `ry-research`;

CREATE TABLE IF NOT EXISTS `task_framework` (
  `framework_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Annual framework ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `framework_name` varchar(200) NOT NULL COMMENT 'Framework name',
  `year` int NOT NULL COMMENT 'Framework year',
  `lead_dept_id` bigint NOT NULL COMMENT 'Lead department ID',
  `overall_goal` varchar(2000) DEFAULT NULL COMMENT 'Overall goal',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 active, 1 disabled',
  `sort` int NOT NULL DEFAULT 0 COMMENT 'Display order',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL COMMENT 'Updated time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'Logical delete flag',
  PRIMARY KEY (`framework_id`),
  KEY `idx_task_framework_group` (`group_id`),
  KEY `idx_task_framework_year` (`year`),
  KEY `idx_task_framework_lead_dept` (`lead_dept_id`),
  KEY `idx_task_framework_list` (`group_id`, `year`, `status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Annual task framework';

CREATE TABLE IF NOT EXISTS `task_framework_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Relation ID',
  `framework_id` bigint NOT NULL COMMENT 'Annual framework ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `dept_id` bigint NOT NULL COMMENT 'Collaborating department ID',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_framework_unit` (`framework_id`, `dept_id`),
  KEY `idx_task_framework_unit_group` (`group_id`),
  KEY `idx_task_framework_unit_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Annual task framework collaborating unit';

CREATE TABLE IF NOT EXISTS `task_info` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
  `framework_id` bigint NOT NULL COMMENT 'Annual framework ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT 'Parent task ID; 0 for root',
  `level` tinyint NOT NULL COMMENT 'Tree level: 1 to 3',
  `task_name` varchar(200) NOT NULL COMMENT 'Task name',
  `task_type` varchar(32) DEFAULT NULL COMMENT 'Task type',
  `description` varchar(2000) DEFAULT NULL COMMENT 'Task description',
  `start_date` date DEFAULT NULL COMMENT 'Start date',
  `deadline` date DEFAULT NULL COMMENT 'Deadline',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '0 draft, 1 running, 2 finished, 3 closed',
  `finish_time` datetime DEFAULT NULL COMMENT 'Finish time',
  `sort` int NOT NULL DEFAULT 0 COMMENT 'Sibling display order',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL COMMENT 'Updated time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'Logical delete flag',
  PRIMARY KEY (`task_id`),
  KEY `idx_task_info_framework` (`framework_id`),
  KEY `idx_task_info_group` (`group_id`),
  KEY `idx_task_info_parent` (`parent_id`),
  KEY `idx_task_info_tree` (`framework_id`, `parent_id`, `sort`, `del_flag`),
  KEY `idx_task_info_deadline` (`deadline`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Research task tree';

CREATE TABLE IF NOT EXISTS `task_deliverable` (
  `deliverable_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Deliverable ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `task_id` bigint NOT NULL COMMENT 'Task ID',
  `deliverable_name` varchar(200) NOT NULL COMMENT 'Deliverable name',
  `requirement` varchar(2000) DEFAULT NULL COMMENT 'Delivery requirement',
  `required_num` int NOT NULL DEFAULT 1 COMMENT 'Required archived submission count',
  `archived_num` int NOT NULL DEFAULT 0 COMMENT 'Current archived submission count',
  `deadline` date DEFAULT NULL COMMENT 'Deadline',
  `is_required` char(1) NOT NULL DEFAULT '1' COMMENT 'Whether required: 1 yes, 0 no',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '0 unfinished, 1 running, 2 finished, 3 closed',
  `finish_time` datetime DEFAULT NULL COMMENT 'Finish time',
  `sort` int NOT NULL DEFAULT 0 COMMENT 'Display order',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL COMMENT 'Updated time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'Logical delete flag',
  PRIMARY KEY (`deliverable_id`),
  KEY `idx_task_deliverable_group` (`group_id`),
  KEY `idx_task_deliverable_task` (`task_id`),
  KEY `idx_task_deliverable_status` (`task_id`, `status`, `is_required`, `del_flag`),
  KEY `idx_task_deliverable_deadline` (`deadline`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task deliverable';

CREATE TABLE IF NOT EXISTS `task_deliverable_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Assignment ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `deliverable_id` bigint NOT NULL COMMENT 'Deliverable ID',
  `user_id` bigint NOT NULL COMMENT 'Responsible user ID',
  `assign_user_id` bigint NOT NULL COMMENT 'Assigning user ID',
  `assign_time` datetime NOT NULL COMMENT 'Assignment time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_deliverable_user` (`deliverable_id`, `user_id`),
  KEY `idx_task_deliverable_user_group` (`group_id`),
  KEY `idx_task_deliverable_user_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Deliverable responsible user';

CREATE TABLE IF NOT EXISTS `task_submission` (
  `submission_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Submission ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `framework_id` bigint NOT NULL COMMENT 'Annual framework ID',
  `task_id` bigint NOT NULL COMMENT 'Task ID',
  `deliverable_id` bigint NOT NULL COMMENT 'Deliverable ID',
  `submission_name` varchar(200) NOT NULL COMMENT 'Submission name',
  `submission_desc` varchar(2000) DEFAULT NULL COMMENT 'Submission description',
  `submit_user_id` bigint NOT NULL COMMENT 'Submitter user ID',
  `submit_dept_id` bigint NOT NULL COMMENT 'Submitter department ID',
  `submit_time` datetime DEFAULT NULL COMMENT 'Submitted time',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '0 draft, 1 pending, 2 rejected, 3 archived',
  `archive_user_id` bigint DEFAULT NULL COMMENT 'Archive reviewer user ID',
  `archive_time` datetime DEFAULT NULL COMMENT 'Archived time',
  `version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT NULL COMMENT 'Created time',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL COMMENT 'Updated time',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'Logical delete flag',
  PRIMARY KEY (`submission_id`),
  KEY `idx_task_submission_group` (`group_id`),
  KEY `idx_task_submission_framework` (`framework_id`),
  KEY `idx_task_submission_task` (`task_id`),
  KEY `idx_task_submission_deliverable` (`deliverable_id`),
  KEY `idx_task_submission_submitter` (`submit_user_id`),
  KEY `idx_task_submission_audit_queue` (`group_id`, `status`, `submit_time`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Deliverable submission';

CREATE TABLE IF NOT EXISTS `task_submission_audit` (
  `audit_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Audit record ID',
  `submission_id` bigint NOT NULL COMMENT 'Submission ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `action` varchar(32) NOT NULL COMMENT 'SUBMIT, APPROVE, REJECT, CANCEL_APPROVE or RESUBMIT',
  `before_status` char(1) DEFAULT NULL COMMENT 'Status before action',
  `after_status` char(1) NOT NULL COMMENT 'Status after action',
  `audit_user_id` bigint NOT NULL COMMENT 'Operator user ID',
  `audit_opinion` varchar(1000) DEFAULT NULL COMMENT 'Audit opinion',
  `audit_time` datetime NOT NULL COMMENT 'Action time',
  PRIMARY KEY (`audit_id`),
  KEY `idx_task_submission_audit_submission` (`submission_id`),
  KEY `idx_task_submission_audit_group` (`group_id`),
  KEY `idx_task_submission_audit_user` (`audit_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Submission status audit history';

CREATE TABLE IF NOT EXISTS `task_attachment` (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Attachment ID',
  `group_id` bigint NOT NULL COMMENT 'Research group ID',
  `submission_id` bigint NOT NULL COMMENT 'Submission ID',
  `file_name` varchar(255) NOT NULL COMMENT 'Stored file name',
  `original_name` varchar(255) NOT NULL COMMENT 'Original file name',
  `file_url` varchar(1000) NOT NULL COMMENT 'Physical file service URL',
  `file_size` bigint DEFAULT NULL COMMENT 'File size in bytes',
  `file_type` varchar(64) DEFAULT NULL COMMENT 'File extension or MIME type',
  `upload_user_id` bigint NOT NULL COMMENT 'Uploader user ID',
  `upload_time` datetime NOT NULL COMMENT 'Upload time',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'Logical delete flag',
  PRIMARY KEY (`attachment_id`),
  KEY `idx_task_attachment_group` (`group_id`),
  KEY `idx_task_attachment_submission` (`submission_id`, `del_flag`),
  KEY `idx_task_attachment_uploader` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task submission attachment';
