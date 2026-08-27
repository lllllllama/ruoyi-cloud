package com.ruoyi.research.service.impl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.mapper.TaskInfoMapper;

@RunWith(MockitoJUnitRunner.class)
public class TaskCompletionServiceImplTest
{
    @InjectMocks private TaskCompletionServiceImpl service;
    @Mock private TaskInfoMapper taskMapper;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserName("auditor");
        when(taskMapper.updateCompletionStatus(20L, "2", "auditor")).thenReturn(1);
        when(taskMapper.updateCompletionStatus(10L, "2", "auditor")).thenReturn(1);
        when(taskMapper.updateCompletionStatus(20L, "1", "auditor")).thenReturn(1);
        when(taskMapper.updateCompletionStatus(10L, "1", "auditor")).thenReturn(1);
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void completedLeafRecursivelyCompletesItsParent()
    {
        when(taskMapper.selectForUpdate(20L)).thenReturn(task(20L, 10L, "1"));
        when(taskMapper.selectForUpdate(10L)).thenReturn(task(10L, 0L, "1"));
        when(taskMapper.countActiveChildren(20L)).thenReturn(0);
        when(taskMapper.countUnfinishedRequiredDeliverables(20L)).thenReturn(0);
        when(taskMapper.countActiveChildren(10L)).thenReturn(1);
        when(taskMapper.countUnfinishedEffectiveChildren(10L)).thenReturn(0);

        service.recalculateFromTask(20L);

        InOrder order = inOrder(taskMapper);
        order.verify(taskMapper).updateCompletionStatus(20L, "2", "auditor");
        order.verify(taskMapper).updateCompletionStatus(10L, "2", "auditor");
    }

    @Test
    public void incompleteLeafRecursivelyRollsBackItsParent()
    {
        when(taskMapper.selectForUpdate(20L)).thenReturn(task(20L, 10L, "2"));
        when(taskMapper.selectForUpdate(10L)).thenReturn(task(10L, 0L, "2"));
        when(taskMapper.countActiveChildren(20L)).thenReturn(0);
        when(taskMapper.countUnfinishedRequiredDeliverables(20L)).thenReturn(1);
        when(taskMapper.countActiveChildren(10L)).thenReturn(1);
        when(taskMapper.countUnfinishedEffectiveChildren(10L)).thenReturn(1);

        service.recalculateFromTask(20L);

        InOrder order = inOrder(taskMapper);
        order.verify(taskMapper).updateCompletionStatus(20L, "1", "auditor");
        order.verify(taskMapper).updateCompletionStatus(10L, "1", "auditor");
    }

    private TaskInfo task(Long taskId, Long parentId, String status)
    {
        TaskInfo task = new TaskInfo();
        task.setTaskId(taskId);
        task.setParentId(parentId);
        task.setStatus(status);
        return task;
    }
}
