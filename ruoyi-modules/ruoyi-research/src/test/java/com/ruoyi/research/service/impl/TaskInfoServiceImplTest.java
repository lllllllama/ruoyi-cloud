package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import java.util.Arrays;
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
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.mapper.TaskFrameworkMapper;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.service.TaskPermissionService;

@RunWith(MockitoJUnitRunner.class)
public class TaskInfoServiceImplTest
{
    @InjectMocks private TaskInfoServiceImpl service;
    @Mock private TaskInfoMapper taskMapper;
    @Mock private TaskFrameworkMapper frameworkMapper;
    @Mock private TaskPermissionService permissionService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("10");
        SecurityContextHolder.setUserName("leader");
        TaskFramework framework = new TaskFramework();
        framework.setFrameworkId(100L);
        framework.setGroupId(1L);
        when(frameworkMapper.selectById(100L)).thenReturn(framework);
        when(taskMapper.insert(any(TaskInfo.class))).thenReturn(1);
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void rootIsLevelOneAndLevelOneMayDirectlyContainLevelThree()
    {
        TaskInfo root = input(null, 0L, null);
        assertEquals(1, service.insert(root));
        assertEquals(Integer.valueOf(1), root.getLevel());

        TaskInfo parent = stored(1L, 0L, 1, 1L, 100L);
        when(taskMapper.selectById(1L)).thenReturn(parent);
        TaskInfo directLevelThree = input(null, 1L, 3);
        assertEquals(1, service.insert(directLevelThree));
        assertEquals(Integer.valueOf(3), directLevelThree.getLevel());
    }

    @Test
    public void fourthLevelIsRejected()
    {
        when(taskMapper.selectById(3L)).thenReturn(stored(3L, 2L, 3, 1L, 100L));
        assertDenied(() -> service.insert(input(null, 3L, null)), "level");
    }

    @Test
    public void parentMustUseTheSameGroupAndFramework()
    {
        when(taskMapper.selectById(2L)).thenReturn(stored(2L, 0L, 1, 2L, 100L));
        assertDenied(() -> service.insert(input(null, 2L, 2)), "same research group");
    }

    @Test
    public void taskCannotMoveBelowItsDescendant()
    {
        TaskInfo root = stored(1L, 0L, 1, 1L, 100L);
        TaskInfo child = stored(2L, 1L, 2, 1L, 100L);
        when(taskMapper.selectById(1L)).thenReturn(root);
        when(taskMapper.selectById(2L)).thenReturn(child);
        when(taskMapper.selectChildren(1L)).thenReturn(Collections.singletonList(child));
        when(taskMapper.selectChildren(2L)).thenReturn(Collections.emptyList());
        assertDenied(() -> service.update(input(1L, 2L, 3)), "descendant");
    }

    @Test
    public void subtreeWithSubmissionCannotMove()
    {
        TaskInfo task = stored(1L, 0L, 1, 1L, 100L);
        TaskInfo newParent = stored(3L, 0L, 1, 1L, 100L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.selectById(3L)).thenReturn(newParent);
        when(taskMapper.selectChildren(1L)).thenReturn(Collections.emptyList());
        when(taskMapper.countSubmissions(1L)).thenReturn(1);
        assertDenied(() -> service.update(input(1L, 3L, 2)), "submissions");
    }

    @Test
    public void onlyLeafTasksRequireDeliverables()
    {
        TaskInfo directory = stored(1L, 0L, 1, 1L, 100L);
        directory.setTaskName("Directory");
        TaskInfo leaf = stored(2L, 1L, 2, 1L, 100L);
        leaf.setTaskName("Leaf");
        when(taskMapper.selectByFrameworkId(100L)).thenReturn(Arrays.asList(directory, leaf));
        when(taskMapper.countActiveChildren(1L)).thenReturn(1);
        when(taskMapper.countActiveChildren(2L)).thenReturn(0);
        when(taskMapper.countActiveDeliverables(2L)).thenReturn(1);
        service.validateFrameworkStructure(100L);

        when(taskMapper.countActiveDeliverables(2L)).thenReturn(0);
        assertDenied(() -> service.validateFrameworkStructure(100L), "Leaf task");
    }

    @Test
    public void userFromGroupACannotReadTaskFromGroupBById()
    {
        TaskInfo foreignTask = stored(90L, 0L, 1, 2L, 200L);
        when(taskMapper.selectById(90L)).thenReturn(foreignTask);
        doThrow(new ServiceException("No permission to view group B task"))
                .when(permissionService).assertCanViewGroup(2L, 10L);
        assertDenied(() -> service.selectById(90L), "No permission");
    }

    private TaskInfo input(Long taskId, Long parentId, Integer level)
    {
        TaskInfo task = stored(taskId, parentId, level, 1L, 100L);
        task.setTaskName("Task");
        return task;
    }

    private TaskInfo stored(Long taskId, Long parentId, Integer level, Long groupId, Long frameworkId)
    {
        TaskInfo task = new TaskInfo();
        task.setTaskId(taskId);
        task.setParentId(parentId);
        task.setLevel(level);
        task.setGroupId(groupId);
        task.setFrameworkId(frameworkId);
        task.setTaskName("Task");
        task.setStatus("0");
        return task;
    }

    private void assertDenied(Runnable action, String messagePart)
    {
        try
        {
            action.run();
            fail("Expected task validation to reject the operation");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains(messagePart));
        }
    }
}
