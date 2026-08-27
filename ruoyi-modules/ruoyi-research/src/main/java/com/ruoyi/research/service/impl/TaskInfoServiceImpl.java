package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.mapper.TaskFrameworkMapper;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.service.TaskInfoService;
import com.ruoyi.research.service.TaskPermissionService;

@Service
public class TaskInfoServiceImpl implements TaskInfoService
{
    private static final String STATUS_DRAFT = "0";

    @Autowired
    private TaskInfoMapper taskMapper;

    @Autowired
    private TaskFrameworkMapper frameworkMapper;

    @Autowired
    private TaskPermissionService permissionService;

    @Override
    public TaskInfo selectById(Long taskId)
    {
        TaskInfo task = requireTask(taskId);
        assertCanView(task.getGroupId());
        return task;
    }

    @Override
    public List<TaskInfo> selectList(TaskInfo query)
    {
        List<Long> allowedGroupIds = permissionService.getAllowedGroupIds(SecurityUtils.getUserId());
        if (allowedGroupIds == null || allowedGroupIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return taskMapper.selectList(query, allowedGroupIds);
    }

    @Override
    public void validateFrameworkStructure(Long frameworkId)
    {
        TaskFramework framework = frameworkMapper.selectById(frameworkId);
        if (framework == null)
        {
            throw new ServiceException("Annual task framework does not exist");
        }
        assertCanView(framework.getGroupId());
        for (TaskInfo task : taskMapper.selectByFrameworkId(frameworkId))
        {
            if (taskMapper.countActiveChildren(task.getTaskId()) == 0
                    && taskMapper.countActiveDeliverables(task.getTaskId()) == 0)
            {
                throw new ServiceException("Leaf task '" + task.getTaskName()
                        + "' must contain at least one deliverable");
            }
        }
    }

    @Override
    @Transactional
    public int insert(TaskInfo task)
    {
        normalize(task);
        validateFramework(task);
        assertCanMaintain(task.getGroupId());
        task.setLevel(resolveLevel(task, null));
        task.setStatus(STATUS_DRAFT);
        task.setFinishTime(null);
        task.setCreateBy(SecurityUtils.getUsername());
        return taskMapper.insert(task);
    }

    @Override
    @Transactional
    public int update(TaskInfo task)
    {
        if (task.getTaskId() == null)
        {
            throw new ServiceException("Task ID is required");
        }
        TaskInfo old = requireTask(task.getTaskId());
        assertCanMaintain(old.getGroupId());
        if (!old.getGroupId().equals(task.getGroupId()) || !old.getFrameworkId().equals(task.getFrameworkId()))
        {
            throw new ServiceException("A task cannot be moved to another research group or annual framework");
        }
        normalize(task);
        validateFramework(task);
        int newLevel = resolveLevel(task, old);
        validateMoveAndUpdateDescendants(task, old, newLevel);
        task.setLevel(newLevel);
        task.setStatus(old.getStatus());
        task.setFinishTime(old.getFinishTime());
        task.setUpdateBy(SecurityUtils.getUsername());
        return taskMapper.update(task);
    }

    @Override
    @Transactional
    public int deleteByIds(Long[] taskIds)
    {
        if (taskIds == null || taskIds.length == 0)
        {
            return 0;
        }
        for (Long taskId : taskIds)
        {
            TaskInfo task = requireTask(taskId);
            assertCanMaintain(task.getGroupId());
            if (taskMapper.countActiveChildren(taskId) > 0)
            {
                throw new ServiceException("Delete child tasks before deleting their parent task");
            }
            if (taskMapper.countSubmissions(taskId) > 0)
            {
                throw new ServiceException("A task with deliverable submissions cannot be deleted");
            }
        }
        return taskMapper.deleteByIds(taskIds, SecurityUtils.getUsername());
    }

    private void normalize(TaskInfo task)
    {
        task.setTaskName(task.getTaskName().trim());
        if (task.getParentId() == null)
        {
            task.setParentId(0L);
        }
        if (StringUtils.isEmpty(task.getStatus()))
        {
            task.setStatus(STATUS_DRAFT);
        }
        if (task.getSort() == null)
        {
            task.setSort(0);
        }
    }

    private int resolveLevel(TaskInfo task, TaskInfo old)
    {
        if (task.getParentId() == 0L)
        {
            if (task.getLevel() != null && task.getLevel() != 1)
            {
                throw new ServiceException("A root task must be level 1");
            }
            return 1;
        }
        if (task.getTaskId() != null && task.getTaskId().equals(task.getParentId()))
        {
            throw new ServiceException("A task cannot be its own parent");
        }
        TaskInfo parent = requireTask(task.getParentId());
        if (!parent.getGroupId().equals(task.getGroupId())
                || !parent.getFrameworkId().equals(task.getFrameworkId()))
        {
            throw new ServiceException("Parent and child tasks must belong to the same research group and annual framework");
        }
        int level;
        if (task.getLevel() != null)
        {
            level = task.getLevel();
        }
        else if (old != null && Objects.equals(old.getParentId(), task.getParentId()))
        {
            level = old.getLevel();
        }
        else
        {
            level = parent.getLevel() + 1;
        }
        if (level <= parent.getLevel() || level > 3)
        {
            throw new ServiceException("A child task level must be greater than its parent level and must not exceed 3");
        }
        return level;
    }

    private void validateMoveAndUpdateDescendants(TaskInfo task, TaskInfo old, int newLevel)
    {
        if (Objects.equals(old.getParentId(), task.getParentId()) && old.getLevel().intValue() == newLevel)
        {
            return;
        }
        List<TaskInfo> subtree = collectSubtree(old);
        Set<Long> subtreeIds = new HashSet<>();
        for (TaskInfo node : subtree)
        {
            subtreeIds.add(node.getTaskId());
            if (taskMapper.countSubmissions(node.getTaskId()) > 0)
            {
                throw new ServiceException("A task subtree with deliverable submissions cannot be moved");
            }
        }
        if (subtreeIds.contains(task.getParentId()))
        {
            throw new ServiceException("A task cannot be moved below one of its descendants");
        }

        int delta = newLevel - old.getLevel();
        for (TaskInfo node : subtree)
        {
            if (node.getTaskId().equals(old.getTaskId()))
            {
                continue;
            }
            int descendantLevel = node.getLevel() + delta;
            if (descendantLevel < 1 || descendantLevel > 3)
            {
                throw new ServiceException("Moving this task would make its subtree exceed level 3");
            }
        }
        for (TaskInfo node : subtree)
        {
            if (!node.getTaskId().equals(old.getTaskId()))
            {
                taskMapper.updateLevel(node.getTaskId(), node.getLevel() + delta, SecurityUtils.getUsername());
            }
        }
    }

    private List<TaskInfo> collectSubtree(TaskInfo root)
    {
        List<TaskInfo> result = new ArrayList<>();
        List<TaskInfo> pending = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        pending.add(root);
        for (int index = 0; index < pending.size(); index++)
        {
            TaskInfo node = pending.get(index);
            if (!visited.add(node.getTaskId()))
            {
                throw new ServiceException("Invalid cyclic task data detected");
            }
            result.add(node);
            pending.addAll(taskMapper.selectChildren(node.getTaskId()));
        }
        return result;
    }

    private void validateFramework(TaskInfo task)
    {
        TaskFramework framework = frameworkMapper.selectById(task.getFrameworkId());
        if (framework == null)
        {
            throw new ServiceException("Annual task framework does not exist");
        }
        if (!framework.getGroupId().equals(task.getGroupId()))
        {
            throw new ServiceException("Task and annual framework must belong to the same research group");
        }
    }

    private TaskInfo requireTask(Long taskId)
    {
        TaskInfo task = taskMapper.selectById(taskId);
        if (task == null)
        {
            throw new ServiceException("Research task does not exist");
        }
        return task;
    }

    private void assertCanView(Long groupId)
    {
        permissionService.assertCanViewGroup(groupId, SecurityUtils.getUserId());
    }

    private void assertCanMaintain(Long groupId)
    {
        permissionService.assertCanMaintainGroup(groupId, SecurityUtils.getUserId());
    }
}
