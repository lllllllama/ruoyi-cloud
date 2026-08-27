package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.service.TaskDeliverableService;
import com.ruoyi.research.service.TaskPermissionService;

@Service
public class TaskDeliverableServiceImpl implements TaskDeliverableService
{
    private static final String STATUS_UNFINISHED = "0";
    private static final String STATUS_RUNNING = "1";
    private static final String STATUS_FINISHED = "2";

    @Autowired
    private TaskDeliverableMapper deliverableMapper;

    @Autowired
    private TaskInfoMapper taskMapper;

    @Autowired
    private TaskPermissionService permissionService;

    @Override
    public TaskDeliverable selectById(Long deliverableId)
    {
        TaskDeliverable deliverable = requireDeliverable(deliverableId);
        assertCanView(deliverable.getGroupId());
        return deliverable;
    }

    @Override
    public List<TaskDeliverable> selectList(TaskDeliverable query)
    {
        List<Long> allowedGroupIds = permissionService.getAllowedGroupIds(SecurityUtils.getUserId());
        if (allowedGroupIds == null || allowedGroupIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return deliverableMapper.selectList(query, allowedGroupIds);
    }

    @Override
    @Transactional
    public int insert(TaskDeliverable deliverable)
    {
        normalize(deliverable);
        requireMatchingTask(deliverable);
        assertCanMaintain(deliverable.getGroupId());
        deliverable.setArchivedNum(0);
        deliverable.setStatus(STATUS_UNFINISHED);
        deliverable.setFinishTime(null);
        deliverable.setCreateBy(SecurityUtils.getUsername());
        return deliverableMapper.insert(deliverable);
    }

    @Override
    @Transactional
    public int update(TaskDeliverable deliverable)
    {
        if (deliverable.getDeliverableId() == null)
        {
            throw new ServiceException("Deliverable ID is required");
        }
        TaskDeliverable old = requireDeliverable(deliverable.getDeliverableId());
        assertCanMaintain(old.getGroupId());
        if (!old.getGroupId().equals(deliverable.getGroupId()) || !old.getTaskId().equals(deliverable.getTaskId()))
        {
            throw new ServiceException("A deliverable cannot be moved to another research group or task");
        }
        normalize(deliverable);
        requireMatchingTask(deliverable);
        deliverable.setArchivedNum(old.getArchivedNum());
        calculateStatus(deliverable);
        deliverable.setUpdateBy(SecurityUtils.getUsername());
        return deliverableMapper.update(deliverable);
    }

    @Override
    @Transactional
    public int delete(Long deliverableId)
    {
        TaskDeliverable deliverable = requireDeliverable(deliverableId);
        assertCanMaintain(deliverable.getGroupId());
        if (deliverableMapper.countSubmissions(deliverableId) > 0)
        {
            throw new ServiceException("A deliverable with submissions cannot be deleted");
        }
        return deliverableMapper.deleteById(deliverableId, SecurityUtils.getUsername());
    }

    private void normalize(TaskDeliverable deliverable)
    {
        deliverable.setDeliverableName(deliverable.getDeliverableName().trim());
        if (deliverable.getRequiredNum() == null || deliverable.getRequiredNum() < 1)
        {
            throw new ServiceException("Required quantity must be at least 1");
        }
        if (StringUtils.isEmpty(deliverable.getIsRequired()))
        {
            deliverable.setIsRequired("1");
        }
        if (!"0".equals(deliverable.getIsRequired()) && !"1".equals(deliverable.getIsRequired()))
        {
            throw new ServiceException("Required flag must be 0 or 1");
        }
        if (deliverable.getSort() == null)
        {
            deliverable.setSort(0);
        }
    }

    private void calculateStatus(TaskDeliverable deliverable)
    {
        int archived = deliverable.getArchivedNum() == null ? 0 : deliverable.getArchivedNum();
        if (archived >= deliverable.getRequiredNum())
        {
            deliverable.setStatus(STATUS_FINISHED);
            deliverable.setFinishTime(new Date());
        }
        else
        {
            deliverable.setStatus(archived > 0 ? STATUS_RUNNING : STATUS_UNFINISHED);
            deliverable.setFinishTime(null);
        }
    }

    private TaskInfo requireMatchingTask(TaskDeliverable deliverable)
    {
        TaskInfo task = taskMapper.selectById(deliverable.getTaskId());
        if (task == null)
        {
            throw new ServiceException("Research task does not exist");
        }
        if (!task.getGroupId().equals(deliverable.getGroupId()))
        {
            throw new ServiceException("Deliverable and task must belong to the same research group");
        }
        return task;
    }

    private TaskDeliverable requireDeliverable(Long deliverableId)
    {
        TaskDeliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null)
        {
            throw new ServiceException("Task deliverable does not exist");
        }
        return deliverable;
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
