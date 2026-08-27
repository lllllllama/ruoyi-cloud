package com.ruoyi.fund.service;

import com.ruoyi.fund.domain.FundAllocationPlan;
import com.ruoyi.fund.domain.FundUsePlan;

public interface FundPermissionService
{
    void assertAdmin(String operation);
    void assertGroupMember(Long groupId, Long userId);
    void assertGroupLeader(Long groupId, Long userId);
    void assertCanAssignAllocation(FundAllocationPlan plan, Long userId);
    void assertCanOperateAllocation(FundAllocationPlan plan, Long userId);
    void assertCanOperateUse(FundUsePlan plan, Long userId);
    void assertOwnRecord(Long submitUserId, Long userId);
    void assertCanAccessBusiness(Long groupId, String businessType, Long userId);
}
