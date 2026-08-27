package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.domain.TaskSubmissionAudit;

public interface TaskSubmissionAuditService
{
    List<TaskSubmissionAudit> selectBySubmissionId(Long submissionId);

    void record(TaskSubmission submission, String action, String beforeStatus,
            String afterStatus, String opinion);
}
