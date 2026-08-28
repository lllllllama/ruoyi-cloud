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
import com.ruoyi.research.util.ResearchSecurityUtils;

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
        if (deliverableId == null || userId == null || ResearchSecurityUtils.isSystemAdmin(userId))
        {
            return false;
        }
        TaskDeliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null || !researchPermissionService.isGroupMember(deliverable.getGroupId(), userId))
        {
            return false;
        }
        if (researchPermissionService.isGroupLeader(deliverable.getGroupId(), userId))
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
        return true;
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
    public boolean canCreateSubmission(Long deliverableId, Long userId)
    {
        if (!canSubmitDeliverable(deliverableId, userId))
        {
            return false;
        }
        TaskDeliverable deliverable = deliverableMapper.selectById(deliverableId);
        return deliverable != null && !hasReachedRequiredQuantity(deliverable);
    }

    @Override
    public void assertCanCreateSubmission(Long deliverableId, Long userId)
    {
        if (!canSubmitDeliverable(deliverableId, userId))
        {
            throw new ServiceException("Current user is not allowed to submit this deliverable");
        }
        TaskDeliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null)
        {
            throw new ServiceException("Task deliverable does not exist");
        }
        if (hasReachedRequiredQuantity(deliverable))
        {
            throw new ServiceException("Deliverable has reached its required archived quantity; new submissions are closed");
        }
    }

    @Override
    public boolean canBeDeliverableAssignee(Long groupId, Long userId)
    {
        return groupId != null && userId != null
                && !ResearchSecurityUtils.isSystemAdmin(userId)
                && researchPermissionService.isGroupMember(groupId, userId)
                && !researchPermissionService.isGroupLeader(groupId, userId);
    }

    @Override
    public void assertCanBeDeliverableAssignee(Long groupId, Long userId)
    {
        if (!canBeDeliverableAssignee(groupId, userId))
        {
            throw new ServiceException("Deliverable assignees must be active non-leader research group members");
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
        if (submission != null && userId != null && userId.equals(submission.getSubmitUserId()))
        {
            throw new ServiceException("A submitter cannot audit their own submission");
        }
        if (!canAuditSubmission(submission, userId))
        {
            throw new ServiceException("Only administrators or research group leaders may audit submissions");
        }
    }

    @Override
    public boolean canAuditSubmission(TaskSubmission submission, Long userId)
    {
        return submission != null && userId != null
                && !userId.equals(submission.getSubmitUserId())
                && researchPermissionService.isGroupLeader(submission.getGroupId(), userId);
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

    private boolean hasReachedRequiredQuantity(TaskDeliverable deliverable)
    {
        int archivedNum = deliverable.getArchivedNum() == null ? 0 : deliverable.getArchivedNum();
        return deliverable.getRequiredNum() != null && archivedNum >= deliverable.getRequiredNum();
    }
}
