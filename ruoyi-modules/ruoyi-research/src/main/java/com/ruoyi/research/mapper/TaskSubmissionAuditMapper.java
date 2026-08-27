package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.TaskSubmissionAudit;

public interface TaskSubmissionAuditMapper
{
    List<TaskSubmissionAudit> selectBySubmissionId(Long submissionId);

    int insert(TaskSubmissionAudit audit);
}
