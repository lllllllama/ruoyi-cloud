package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskAttachment;

public interface TaskAttachmentMapper
{
    TaskAttachment selectById(Long attachmentId);

    List<TaskAttachment> selectBySubmissionId(Long submissionId);

    int countBySubmissionId(Long submissionId);

    int insert(TaskAttachment attachment);

    int deleteById(@Param("attachmentId") Long attachmentId,
            @Param("uploadUserId") Long uploadUserId);

    int deleteBySubmissionId(Long submissionId);
}
