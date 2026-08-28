package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskDeliverableUserMapper;
import com.ruoyi.research.service.ResearchPermissionService;

@RunWith(MockitoJUnitRunner.class)
public class TaskPermissionServiceImplTest
{
    @InjectMocks private TaskPermissionServiceImpl service;
    @Mock private TaskDeliverableMapper deliverableMapper;
    @Mock private TaskDeliverableUserMapper deliverableUserMapper;
    @Mock private ResearchPermissionService researchPermissionService;

    @Before
    public void setUp()
    {
        TaskDeliverable deliverable = new TaskDeliverable();
        deliverable.setDeliverableId(30L);
        deliverable.setGroupId(1L);
        when(deliverableMapper.selectById(30L)).thenReturn(deliverable);
        when(researchPermissionService.isGroupMember(1L, 10L)).thenReturn(true);
        when(researchPermissionService.isGroupMember(1L, 11L)).thenReturn(true);
        when(researchPermissionService.isGroupMember(1L, 12L)).thenReturn(true);
        when(researchPermissionService.isGroupMember(1L, 13L)).thenReturn(true);
    }

    @Test
    public void assignedDeliverableOnlyAllowsExplicitAssignee()
    {
        when(deliverableUserMapper.countByDeliverableId(30L)).thenReturn(1);
        when(deliverableUserMapper.countByDeliverableAndUser(
                argThat(value -> value.getDeliverableId().equals(30L) && value.getUserId().equals(10L))))
                .thenReturn(1);
        assertTrue(service.canSubmitDeliverable(30L, 10L));
        assertFalse(service.canSubmitDeliverable(30L, 11L));
    }

    @Test
    public void unassignedDeliverableAllowsAllNonLeaderMembers()
    {
        when(deliverableUserMapper.countByDeliverableId(30L)).thenReturn(0);
        when(researchPermissionService.isGroupLeader(1L, 10L)).thenReturn(true);
        assertFalse(service.canSubmitDeliverable(30L, 10L));
        assertTrue(service.canSubmitDeliverable(30L, 11L));
        assertTrue(service.canSubmitDeliverable(30L, 12L));
        assertTrue(service.canSubmitDeliverable(30L, 13L));
        assertFalse(service.canSubmitDeliverable(30L, 1L));
    }

    @Test
    public void leaderCannotSubmitEvenWhenAssigned()
    {
        when(researchPermissionService.isGroupLeader(1L, 10L)).thenReturn(true);
        assertFalse(service.canSubmitDeliverable(30L, 10L));
    }

    @Test
    public void adminCannotSubmitEvenWhenAssigned()
    {
        assertFalse(service.canSubmitDeliverable(30L, 1L));
    }

    @Test
    public void completedDeliverableRejectsNewSubmissionCreation()
    {
        TaskDeliverable completed = new TaskDeliverable();
        completed.setDeliverableId(30L);
        completed.setGroupId(1L);
        completed.setRequiredNum(2);
        completed.setArchivedNum(2);
        when(deliverableMapper.selectById(30L)).thenReturn(completed);
        when(deliverableUserMapper.countByDeliverableId(30L)).thenReturn(0);

        assertTrue(service.canSubmitDeliverable(30L, 11L));
        assertFalse(service.canCreateSubmission(30L, 11L));
    }

    @Test
    public void outsiderCannotSubmit()
    {
        when(researchPermissionService.isGroupMember(1L, 20L)).thenReturn(false);
        assertFalse(service.canSubmitDeliverable(30L, 20L));
    }

    @Test
    public void inactiveMemberCannotSubmit()
    {
        when(researchPermissionService.isGroupMember(1L, 21L)).thenReturn(false);
        assertFalse(service.canSubmitDeliverable(30L, 21L));
    }

    @Test
    public void submissionVisibilityCoversOwnerLeaderArchivedMemberAndOutsider()
    {
        TaskSubmission submission = submission("1", 10L);
        service.assertCanViewSubmission(submission, 10L);

        when(researchPermissionService.isGroupLeader(1L, 11L)).thenReturn(true);
        service.assertCanViewSubmission(submission, 11L);

        submission.setStatus("3");
        service.assertCanViewSubmission(submission, 12L);

        expectDenied(() -> service.assertCanViewSubmission(submission, 20L));
    }

    @Test
    public void leaderCanAuditMemberSubmission()
    {
        TaskSubmission submission = submission("1", 10L);
        when(researchPermissionService.isGroupLeader(1L, 11L)).thenReturn(true);
        service.assertCanAuditSubmission(submission, 11L);
    }

    @Test
    public void adminCanAuditMemberSubmission()
    {
        TaskSubmission submission = submission("1", 10L);
        when(researchPermissionService.isGroupLeader(1L, 1L)).thenReturn(true);
        service.assertCanAuditSubmission(submission, 1L);
    }

    @Test
    public void submitterCannotAuditOwnSubmission()
    {
        TaskSubmission submission = submission("1", 10L);
        expectDenied(() -> service.assertCanAuditSubmission(submission, 10L));
    }

    @Test
    public void nonLeaderCannotAudit()
    {
        TaskSubmission submission = submission("1", 10L);
        expectDenied(() -> service.assertCanAuditSubmission(submission, 12L));
    }

    @Test
    public void submissionOwnerCheckUsesServerIdentity()
    {
        TaskSubmission submission = submission("1", 10L);
        service.assertSubmissionOwner(submission, 10L);
        expectDenied(() -> service.assertSubmissionOwner(submission, 11L));
    }

    private TaskSubmission submission(String status, Long ownerId)
    {
        TaskSubmission submission = new TaskSubmission();
        submission.setGroupId(1L);
        submission.setSubmitUserId(ownerId);
        submission.setStatus(status);
        return submission;
    }

    private void expectDenied(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected permission rejection");
        }
        catch (ServiceException expected)
        {
            // Expected.
        }
    }
}
