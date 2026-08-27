package com.ruoyi.research.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.domain.TaskSubmissionAudit;
import com.ruoyi.research.mapper.TaskSubmissionAuditMapper;
import com.ruoyi.research.mapper.TaskSubmissionMapper;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.research.service.TaskSubmissionAuditService;

@Service
public class TaskSubmissionAuditServiceImpl implements TaskSubmissionAuditService
{
    @Autowired
    private TaskSubmissionAuditMapper auditMapper;

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    @Autowired
    private TaskPermissionService permissionService;

    @Override
    public List<TaskSubmissionAudit> selectBySubmissionId(Long submissionId)
    {
        TaskSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null)
        {
            throw new ServiceException("Deliverable submission does not exist");
        }
        permissionService.assertCanViewSubmission(submission, SecurityUtils.getUserId());
        return auditMapper.selectBySubmissionId(submissionId);
    }

    @Override
    public void record(TaskSubmission submission, String action, String beforeStatus,
            String afterStatus, String opinion)
    {
        TaskSubmissionAudit audit = new TaskSubmissionAudit();
        audit.setSubmissionId(submission.getSubmissionId());
        audit.setGroupId(submission.getGroupId());
        audit.setAction(action);
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setAuditUserId(SecurityUtils.getUserId());
        audit.setAuditOpinion(opinion == null ? null : opinion.trim());
        audit.setAuditTime(new Date());
        auditMapper.insert(audit);
    }
}
