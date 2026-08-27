package com.ruoyi.research.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskAttachment;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.mapper.TaskAttachmentMapper;
import com.ruoyi.research.mapper.TaskSubmissionMapper;
import com.ruoyi.research.service.TaskAttachmentService;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.system.api.RemoteFileService;

@Service
public class TaskAttachmentServiceImpl implements TaskAttachmentService
{
    private static final int MAX_ATTACHMENTS = 20;
    private static final String STATUS_DRAFT = "0";
    private static final String STATUS_REJECTED = "2";

    @Autowired
    private TaskAttachmentMapper attachmentMapper;

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    @Autowired
    private TaskPermissionService permissionService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Override
    public List<TaskAttachment> selectBySubmissionId(Long submissionId)
    {
        TaskSubmission submission = requireSubmission(submissionId);
        assertCanView(submission);
        return attachmentMapper.selectBySubmissionId(submissionId);
    }

    @Override
    @Transactional
    public int insert(Long submissionId, TaskAttachment attachment)
    {
        TaskSubmission submission = requireSubmission(submissionId);
        assertEditableOwner(submission);
        if (attachmentMapper.countBySubmissionId(submissionId) >= MAX_ATTACHMENTS)
        {
            throw new ServiceException("A submission may contain at most 20 attachments");
        }
        normalize(attachment);
        attachment.setAttachmentId(null);
        attachment.setGroupId(submission.getGroupId());
        attachment.setSubmissionId(submissionId);
        attachment.setUploadUserId(SecurityUtils.getUserId());
        attachment.setUploadTime(new Date());
        attachment.setDelFlag("0");
        return attachmentMapper.insert(attachment);
    }

    @Override
    @Transactional
    public int delete(Long attachmentId)
    {
        TaskAttachment attachment = requireAttachment(attachmentId);
        TaskSubmission submission = requireSubmission(attachment.getSubmissionId());
        assertEditableOwner(submission);
        return attachmentMapper.deleteById(attachmentId, SecurityUtils.getUserId());
    }

    @Override
    @Transactional
    public void deleteBySubmissionId(Long submissionId)
    {
        attachmentMapper.deleteBySubmissionId(submissionId);
    }

    @Override
    public ResponseEntity<byte[]> download(Long attachmentId)
    {
        TaskAttachment attachment = requireAttachment(attachmentId);
        TaskSubmission submission = requireSubmission(attachment.getSubmissionId());
        assertCanView(submission);
        ResponseEntity<byte[]> remote = remoteFileService.download(attachment.getFileUrl(), SecurityConstants.INNER);
        if (remote == null || !remote.getStatusCode().is2xxSuccessful() || remote.getBody() == null)
        {
            throw new ServiceException("File service download failed");
        }
        byte[] content = remote.getBody();
        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = remote.getHeaders().getContentType();
        headers.setContentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType);
        headers.setContentLength(content.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition(attachment.getOriginalName()));
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private void normalize(TaskAttachment attachment)
    {
        attachment.setFileName(requireText(attachment.getFileName(), "File name"));
        attachment.setOriginalName(requireText(attachment.getOriginalName(), "Original file name"));
        attachment.setFileUrl(requireText(attachment.getFileUrl(), "File URL"));
        if (attachment.getFileName().length() > 255 || attachment.getOriginalName().length() > 255
                || attachment.getFileUrl().length() > 1000)
        {
            throw new ServiceException("Attachment metadata exceeds the allowed length");
        }
        if (attachment.getFileSize() != null && attachment.getFileSize() < 0)
        {
            throw new ServiceException("File size must not be negative");
        }
        if (attachment.getFileType() != null && attachment.getFileType().length() > 64)
        {
            throw new ServiceException("File type must not exceed 64 characters");
        }
    }

    private String requireText(String value, String label)
    {
        if (value == null || value.trim().isEmpty())
        {
            throw new ServiceException(label + " is required");
        }
        return value.trim();
    }

    private void assertEditableOwner(TaskSubmission submission)
    {
        permissionService.assertSubmissionOwner(submission, SecurityUtils.getUserId());
        if (!STATUS_DRAFT.equals(submission.getStatus()) && !STATUS_REJECTED.equals(submission.getStatus()))
        {
            throw new ServiceException("Attachments are locked after submission");
        }
    }

    private void assertCanView(TaskSubmission submission)
    {
        permissionService.assertCanViewSubmission(submission, SecurityUtils.getUserId());
    }

    private TaskSubmission requireSubmission(Long submissionId)
    {
        TaskSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null)
        {
            throw new ServiceException("Deliverable submission does not exist");
        }
        return submission;
    }

    private TaskAttachment requireAttachment(Long attachmentId)
    {
        TaskAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null)
        {
            throw new ServiceException("Task attachment does not exist");
        }
        return attachment;
    }

    private String disposition(String fileName)
    {
        try
        {
            return "attachment; filename*=UTF-8''" + FileUtils.percentEncode(fileName);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new ServiceException("Attachment name encoding failed");
        }
    }
}
