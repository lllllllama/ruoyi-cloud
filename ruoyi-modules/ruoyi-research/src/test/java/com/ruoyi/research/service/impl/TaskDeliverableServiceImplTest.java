package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.service.TaskPermissionService;

@RunWith(MockitoJUnitRunner.class)
public class TaskDeliverableServiceImplTest
{
    @InjectMocks private TaskDeliverableServiceImpl service;
    @Mock private TaskDeliverableMapper deliverableMapper;
    @Mock private TaskInfoMapper taskMapper;
    @Mock private TaskPermissionService permissionService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("10");
        SecurityContextHolder.setUserName("leader");
        TaskInfo task = new TaskInfo();
        task.setTaskId(20L);
        task.setGroupId(1L);
        when(taskMapper.selectById(20L)).thenReturn(task);
        when(deliverableMapper.insert(any(TaskDeliverable.class))).thenReturn(1);
        when(deliverableMapper.update(any(TaskDeliverable.class))).thenReturn(1);
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void insertIgnoresClientArchivedCountAndStatus()
    {
        TaskDeliverable deliverable = input(null, 2);
        deliverable.setArchivedNum(99);
        deliverable.setStatus("2");
        assertEquals(1, service.insert(deliverable));
        assertEquals(Integer.valueOf(0), deliverable.getArchivedNum());
        assertEquals("0", deliverable.getStatus());
        assertNull(deliverable.getFinishTime());
    }

    @Test
    public void requiredQuantityRecalculatesFinishedAndRunningStatus()
    {
        TaskDeliverable old = input(30L, 2);
        old.setArchivedNum(2);
        old.setStatus("2");
        when(deliverableMapper.selectById(30L)).thenReturn(old);

        TaskDeliverable raisedTarget = input(30L, 3);
        assertEquals(1, service.update(raisedTarget));
        assertEquals(Integer.valueOf(2), raisedTarget.getArchivedNum());
        assertEquals("1", raisedTarget.getStatus());
        assertNull(raisedTarget.getFinishTime());

        TaskDeliverable reachedTarget = input(30L, 2);
        assertEquals(1, service.update(reachedTarget));
        assertEquals("2", reachedTarget.getStatus());
        assertNotNull(reachedTarget.getFinishTime());
    }

    private TaskDeliverable input(Long deliverableId, int requiredNum)
    {
        TaskDeliverable deliverable = new TaskDeliverable();
        deliverable.setDeliverableId(deliverableId);
        deliverable.setGroupId(1L);
        deliverable.setTaskId(20L);
        deliverable.setDeliverableName("Deliverable");
        deliverable.setRequiredNum(requiredNum);
        deliverable.setIsRequired("1");
        return deliverable;
    }
}
