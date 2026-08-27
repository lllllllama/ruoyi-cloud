package com.ruoyi.research.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.ruoyi.research.domain.TaskAttachment;

public interface TaskAttachmentService
{
    List<TaskAttachment> selectBySubmissionId(Long submissionId);

    int insert(Long submissionId, TaskAttachment attachment);

    int delete(Long attachmentId);

    void deleteBySubmissionId(Long submissionId);

    ResponseEntity<byte[]> download(Long attachmentId);
}
