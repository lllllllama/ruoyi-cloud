package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskSubmission;

public interface TaskSubmissionMapper
{
    TaskSubmission selectById(Long submissionId);

    List<TaskSubmission> selectList(@Param("query") TaskSubmission query,
            @Param("userId") Long userId, @Param("admin") boolean admin);

    int insert(TaskSubmission submission);

    int updateDraft(TaskSubmission submission);

    int deleteDraft(@Param("submissionId") Long submissionId,
            @Param("version") Integer version, @Param("updateBy") String updateBy);
}
