package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskSubmission;

public interface TaskSubmissionMapper
{
    TaskSubmission selectById(Long submissionId);

    TaskSubmission selectForUpdate(Long submissionId);

    List<TaskSubmission> selectList(@Param("query") TaskSubmission query,
            @Param("userId") Long userId, @Param("admin") boolean admin);

    List<TaskSubmission> selectMine(@Param("deliverableId") Long deliverableId,
            @Param("userId") Long userId);

    int insert(TaskSubmission submission);

    int updateDraft(TaskSubmission submission);

    int deleteDraft(@Param("submissionId") Long submissionId,
            @Param("version") Integer version, @Param("updateBy") String updateBy);

    int submit(@Param("submissionId") Long submissionId, @Param("version") Integer version,
            @Param("updateBy") String updateBy);

    int resubmit(@Param("submissionId") Long submissionId, @Param("version") Integer version,
            @Param("updateBy") String updateBy);

    int approve(@Param("submissionId") Long submissionId, @Param("version") Integer version,
            @Param("archiveUserId") Long archiveUserId, @Param("updateBy") String updateBy);

    int reject(@Param("submissionId") Long submissionId, @Param("version") Integer version,
            @Param("updateBy") String updateBy);

    int cancelApprove(@Param("submissionId") Long submissionId, @Param("version") Integer version,
            @Param("updateBy") String updateBy);
}
