package com.ruoyi.research.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskDeliverableUser;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskDeliverableUserMapper;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.TaskPermissionService;

@Service
public class TaskPermissionServiceImpl implements TaskPermissionService
{
    @Autowired
    private TaskDeliverableMapper deliverableMapper;

    @Autowired
    private TaskDeliverableUserMapper deliverableUserMapper;

    @Autowired
    private ResearchPermissionService researchPermissionService;

    @Override
    public boolean canSubmitDeliverable(Long deliverableId, Long userId)
    {
        if (deliverableId == null || userId == null)
        {
            return false;
        }
        TaskDeliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null || !researchPermissionService.isGroupMember(deliverable.getGroupId(), userId))
        {
            return false;
        }
        int assigneeCount = deliverableUserMapper.countByDeliverableId(deliverableId);
        if (assigneeCount > 0)
        {
            TaskDeliverableUser relation = new TaskDeliverableUser();
            relation.setDeliverableId(deliverableId);
            relation.setUserId(userId);
            return deliverableUserMapper.countByDeliverableAndUser(relation) > 0;
        }
        return researchPermissionService.isGroupLeader(deliverable.getGroupId(), userId)
                || researchPermissionService.isGroupCore(deliverable.getGroupId(), userId);
    }

    @Override
    public void assertCanSubmitDeliverable(Long deliverableId, Long userId)
    {
        if (!canSubmitDeliverable(deliverableId, userId))
        {
            throw new ServiceException("Current user is not allowed to submit this deliverable");
        }
    }
}
