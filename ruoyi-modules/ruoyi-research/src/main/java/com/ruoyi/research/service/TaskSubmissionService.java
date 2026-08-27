package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskSubmission;

public interface TaskSubmissionService
{
    TaskSubmission selectById(Long submissionId);

    List<TaskSubmission> selectList(TaskSubmission query);

    int insertDraft(TaskSubmission submission);

    int updateDraft(TaskSubmission submission);

    int deleteDraft(Long submissionId);
}
