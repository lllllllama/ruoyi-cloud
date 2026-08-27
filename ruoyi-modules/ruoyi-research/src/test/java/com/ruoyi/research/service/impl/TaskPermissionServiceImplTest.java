package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.research.domain.TaskDeliverable;
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
    public void unassignedDeliverableAllowsLeaderAndCoreOnly()
    {
        when(deliverableUserMapper.countByDeliverableId(30L)).thenReturn(0);
        when(researchPermissionService.isGroupLeader(1L, 10L)).thenReturn(true);
        when(researchPermissionService.isGroupCore(1L, 11L)).thenReturn(true);
        assertTrue(service.canSubmitDeliverable(30L, 10L));
        assertTrue(service.canSubmitDeliverable(30L, 11L));
        assertFalse(service.canSubmitDeliverable(30L, 12L));
        assertFalse(service.canSubmitDeliverable(30L, 13L));
    }

    @Test
    public void inactiveOrUnrelatedUserCannotSubmitEvenIfAssigned()
    {
        when(researchPermissionService.isGroupMember(1L, 20L)).thenReturn(false);
        assertFalse(service.canSubmitDeliverable(30L, 20L));
    }
}
