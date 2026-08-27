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

    void submit(Long submissionId);

    void approve(Long submissionId, String opinion);

    void reject(Long submissionId, String opinion);

    void resubmit(Long submissionId, String opinion);

    void cancelApprove(Long submissionId, String opinion);
}
