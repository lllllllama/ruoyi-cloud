package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.TaskAttachment;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.mapper.TaskAttachmentMapper;
import com.ruoyi.research.mapper.TaskSubmissionMapper;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.system.api.RemoteFileService;

@RunWith(MockitoJUnitRunner.class)
public class TaskAttachmentServiceImplTest
{
    @InjectMocks private TaskAttachmentServiceImpl service;
    @Mock private TaskAttachmentMapper attachmentMapper;
    @Mock private TaskSubmissionMapper submissionMapper;
    @Mock private TaskPermissionService permissionService;
    @Mock private RemoteFileService remoteFileService;

    @Before
    public void setUp()
    {
        login(10L);
        when(attachmentMapper.insert(any(TaskAttachment.class))).thenReturn(1);
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void editableOwnerCreatesAttachmentWithServerOwnedIdentity()
    {
        when(submissionMapper.selectById(40L)).thenReturn(submission("0"));
        TaskAttachment attachment = attachment();
        attachment.setGroupId(999L);
        attachment.setUploadUserId(999L);
        assertEquals(1, service.insert(40L, attachment));
        assertEquals(Long.valueOf(1L), attachment.getGroupId());
        assertEquals(Long.valueOf(40L), attachment.getSubmissionId());
        assertEquals(Long.valueOf(10L), attachment.getUploadUserId());
        assertEquals("0", attachment.getDelFlag());
    }

    @Test
    public void archivedAttachmentAllowsGroupMemberButRejectsOutsider()
    {
        when(submissionMapper.selectById(40L)).thenReturn(submission("3"));
        when(attachmentMapper.selectBySubmissionId(40L)).thenReturn(Collections.emptyList());
        login(20L);
        service.selectBySubmissionId(40L);

        login(21L);
        doThrow(new ServiceException("No permission to view this submission"))
                .when(permissionService).assertCanViewSubmission(any(TaskSubmission.class), eq(21L));
        try
        {
            service.selectBySubmissionId(40L);
            fail("Outsider must not access archived attachments");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("No permission"));
        }
    }

    private TaskSubmission submission(String status)
    {
        TaskSubmission submission = new TaskSubmission();
        submission.setSubmissionId(40L);
        submission.setGroupId(1L);
        submission.setSubmitUserId(10L);
        submission.setStatus(status);
        return submission;
    }

    private TaskAttachment attachment()
    {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setFileName("stored.pdf");
        attachment.setOriginalName("result.pdf");
        attachment.setFileUrl("/profile/upload/result.pdf");
        return attachment;
    }

    private void login(Long userId)
    {
        SecurityContextHolder.remove();
        SecurityContextHolder.setUserId(String.valueOf(userId));
        SecurityContextHolder.setUserName("user" + userId);
    }
}
