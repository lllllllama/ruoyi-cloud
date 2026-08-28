package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskSubmission;

public interface TaskPermissionService
{
    List<Long> getAllowedGroupIds(Long userId);

    void assertCanViewGroup(Long groupId, Long userId);

    void assertCanMaintainGroup(Long groupId, Long userId);

    void assertGroupMember(Long groupId, Long userId);

    boolean canSubmitDeliverable(Long deliverableId, Long userId);

    void assertCanSubmitDeliverable(Long deliverableId, Long userId);

    boolean canCreateSubmission(Long deliverableId, Long userId);

    void assertCanCreateSubmission(Long deliverableId, Long userId);

    boolean canBeDeliverableAssignee(Long groupId, Long userId);

    void assertCanBeDeliverableAssignee(Long groupId, Long userId);

    void assertSubmissionOwner(TaskSubmission submission, Long userId);

    boolean canAuditSubmission(TaskSubmission submission, Long userId);

    void assertCanAuditSubmission(TaskSubmission submission, Long userId);

    void assertCanViewSubmission(TaskSubmission submission, Long userId);
}
