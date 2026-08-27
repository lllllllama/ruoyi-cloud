package com.ruoyi.research.service;

public interface TaskPermissionService
{
    boolean canSubmitDeliverable(Long deliverableId, Long userId);

    void assertCanSubmitDeliverable(Long deliverableId, Long userId);
}
