package com.ruoyi.research.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskDeliverableUser;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskDeliverableUserMapper;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.TaskPermissionService;

@Service
public class TaskPermissionServiceImpl implements TaskPermissionService
{
    private static final String STATUS_ARCHIVED = "3";

    @Autowired
    private TaskDeliverableMapper deliverableMapper;

    @Autowired
    private TaskDeliverableUserMapper deliverableUserMapper;

    @Autowired
    private ResearchPermissionService researchPermissionService;

    @Override
    public List<Long> getAllowedGroupIds(Long userId)
    {
        return researchPermissionService.getAllowedGroupIds(userId);
    }

    @Override
    public void assertCanViewGroup(Long groupId, Long userId)
    {
        if (!researchPermissionService.canViewGroup(groupId, userId))
        {
            throw new ServiceException("No permission to view this research group task data");
        }
    }

    @Override
    public void assertCanMaintainGroup(Long groupId, Long userId)
    {
        if (!researchPermissionService.isGroupLeader(groupId, userId))
        {
            throw new ServiceException("Only administrators or research group leaders may maintain task data");
        }
    }

    @Override
    public void assertGroupMember(Long groupId, Long userId)
    {
        if (!researchPermissionService.isGroupMember(groupId, userId))
        {
            throw new ServiceException("User is not an active member of this research group");
        }
    }

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

    @Override
    public void assertSubmissionOwner(TaskSubmission submission, Long userId)
    {
        if (submission == null || userId == null || !userId.equals(submission.getSubmitUserId()))
        {
            throw new ServiceException("Only the submitter may modify this submission");
        }
    }

    @Override
    public void assertCanAuditSubmission(TaskSubmission submission, Long userId)
    {
        if (submission == null || !researchPermissionService.isGroupLeader(submission.getGroupId(), userId))
        {
            throw new ServiceException("Only administrators or research group leaders may audit submissions");
        }
    }

    @Override
    public void assertCanViewSubmission(TaskSubmission submission, Long userId)
    {
        if (submission != null && userId != null
                && (userId.equals(submission.getSubmitUserId())
                    || researchPermissionService.isGroupLeader(submission.getGroupId(), userId)
                    || STATUS_ARCHIVED.equals(submission.getStatus())
                        && researchPermissionService.isGroupMember(submission.getGroupId(), userId)))
        {
            return;
        }
        throw new ServiceException("No permission to view this submission");
    }
}
