package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskDeliverableUser;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskDeliverableUserMapper;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.TaskDeliverableUserService;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class TaskDeliverableUserServiceImpl implements TaskDeliverableUserService
{
    @Autowired
    private TaskDeliverableUserMapper userMapper;

    @Autowired
    private TaskDeliverableMapper deliverableMapper;

    @Autowired
    private ResearchPermissionService permissionService;

    @Autowired
    private ResearchOrgService orgService;

    @Override
    public List<TaskDeliverableUser> selectByDeliverableId(Long deliverableId)
    {
        TaskDeliverable deliverable = requireDeliverable(deliverableId);
        assertCanView(deliverable.getGroupId());
        List<TaskDeliverableUser> relations = userMapper.selectByDeliverableId(deliverableId);
        for (TaskDeliverableUser relation : relations)
        {
            FundUserOption user = orgService.getUser(relation.getUserId());
            relation.setUserName(user.getUserName());
            relation.setNickName(user.getNickName());
        }
        return relations;
    }

    @Override
    @Transactional
    public int assign(Long deliverableId, List<Long> userIds)
    {
        TaskDeliverable deliverable = requireDeliverable(deliverableId);
        assertCanMaintain(deliverable.getGroupId());
        Set<Long> uniqueUserIds = normalizeUserIds(userIds);
        List<TaskDeliverableUser> relations = new ArrayList<>();
        Long operatorId = SecurityUtils.getUserId();
        Date now = new Date();
        for (Long userId : uniqueUserIds)
        {
            orgService.getUser(userId);
            if (!permissionService.isGroupMember(deliverable.getGroupId(), userId))
            {
                throw new ServiceException("Deliverable assignee must be an active research group member");
            }
            TaskDeliverableUser relation = new TaskDeliverableUser();
            relation.setGroupId(deliverable.getGroupId());
            relation.setDeliverableId(deliverableId);
            relation.setUserId(userId);
            relation.setAssignUserId(operatorId);
            relation.setAssignTime(now);
            relations.add(relation);
        }
        userMapper.deleteByDeliverableId(deliverableId);
        if (!relations.isEmpty())
        {
            userMapper.batchInsert(relations);
        }
        return 1;
    }

    private Set<Long> normalizeUserIds(List<Long> userIds)
    {
        if (userIds == null)
        {
            throw new ServiceException("Assignee list is required");
        }
        Set<Long> uniqueUserIds = new LinkedHashSet<>();
        for (Long userId : userIds)
        {
            if (userId == null)
            {
                throw new ServiceException("Assignee user ID is required");
            }
            uniqueUserIds.add(userId);
        }
        return uniqueUserIds;
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
        if (!permissionService.canViewGroup(groupId, SecurityUtils.getUserId()))
        {
            throw new ServiceException("No permission to view deliverable assignees");
        }
    }

    private void assertCanMaintain(Long groupId)
    {
        if (!permissionService.isGroupLeader(groupId, SecurityUtils.getUserId()))
        {
            throw new ServiceException("Only administrators or research group leaders may assign deliverable owners");
        }
    }
}
