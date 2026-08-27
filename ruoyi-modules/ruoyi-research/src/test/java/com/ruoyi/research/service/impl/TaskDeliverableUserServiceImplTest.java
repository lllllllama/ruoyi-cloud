package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskDeliverableUserMapper;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.system.api.domain.FundUserOption;

@RunWith(MockitoJUnitRunner.class)
public class TaskDeliverableUserServiceImplTest
{
    @InjectMocks private TaskDeliverableUserServiceImpl service;
    @Mock private TaskDeliverableUserMapper userMapper;
    @Mock private TaskDeliverableMapper deliverableMapper;
    @Mock private ResearchPermissionService permissionService;
    @Mock private ResearchOrgService orgService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("10");
        SecurityContextHolder.setUserName("leader");
        TaskDeliverable deliverable = new TaskDeliverable();
        deliverable.setDeliverableId(30L);
        deliverable.setGroupId(1L);
        when(deliverableMapper.selectById(30L)).thenReturn(deliverable);
        when(permissionService.isGroupLeader(1L, 10L)).thenReturn(true);
        when(orgService.getUser(any(Long.class))).thenReturn(new FundUserOption());
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void duplicateAssigneesAreStoredOnceAndMustBeGroupMembers()
    {
        when(permissionService.isGroupMember(1L, 20L)).thenReturn(true);
        when(permissionService.isGroupMember(1L, 21L)).thenReturn(true);
        when(userMapper.batchInsert(any())).thenReturn(2);
        assertEquals(1, service.assign(30L, Arrays.asList(20L, 20L, 21L)));
        verify(userMapper).deleteByDeliverableId(30L);
        verify(userMapper).batchInsert(argThat(relations -> relations.size() == 2));

        when(permissionService.isGroupMember(1L, 22L)).thenReturn(false);
        try
        {
            service.assign(30L, Arrays.asList(22L));
            fail("An outsider must not be assigned");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("research group member"));
        }
    }
}
