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
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.research.service.TaskSubmissionAuditService;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class TaskSubmissionAuditServiceImpl implements TaskSubmissionAuditService
{
    @Autowired
    private TaskSubmissionAuditMapper auditMapper;

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    @Autowired
    private TaskPermissionService permissionService;

    @Autowired
    private ResearchOrgService orgService;

    @Override
    public List<TaskSubmissionAudit> selectBySubmissionId(Long submissionId)
    {
        TaskSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null)
        {
            throw new ServiceException("Deliverable submission does not exist");
        }
        permissionService.assertCanViewSubmission(submission, SecurityUtils.getUserId());
        List<TaskSubmissionAudit> audits = auditMapper.selectBySubmissionId(submissionId);
        for (TaskSubmissionAudit audit : audits)
        {
            enrichAuditUser(audit);
        }
        return audits;
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

    private void enrichAuditUser(TaskSubmissionAudit audit)
    {
        if (audit.getAuditUserId() == null)
        {
            return;
        }
        try
        {
            FundUserOption user = orgService.getUser(audit.getAuditUserId());
            if (user != null)
            {
                audit.setAuditUserName(user.getNickName());
            }
        }
        catch (ServiceException ignored)
        {
            // Historical audit rows remain readable even if the operator account no longer exists.
        }
    }
}
