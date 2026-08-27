package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.List;
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
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.TaskInfoService;

@Service
public class TaskInfoServiceImpl implements TaskInfoService
{
    private static final String STATUS_DRAFT = "0";

    @Autowired
    private TaskInfoMapper taskMapper;

    @Autowired
    private TaskFrameworkMapper frameworkMapper;

    @Autowired
    private ResearchPermissionService permissionService;

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
    @Transactional
    public int insert(TaskInfo task)
    {
        normalize(task);
        validateFramework(task);
        assertCanMaintain(task.getGroupId());
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
        if (task.getLevel() == null)
        {
            task.setLevel(task.getParentId() == 0L ? 1 : 2);
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
        if (!permissionService.canViewGroup(groupId, SecurityUtils.getUserId()))
        {
            throw new ServiceException("No permission to view this task");
        }
    }

    private void assertCanMaintain(Long groupId)
    {
        if (!permissionService.isGroupLeader(groupId, SecurityUtils.getUserId()))
        {
            throw new ServiceException("Only administrators or research group leaders may maintain tasks");
        }
    }
}
