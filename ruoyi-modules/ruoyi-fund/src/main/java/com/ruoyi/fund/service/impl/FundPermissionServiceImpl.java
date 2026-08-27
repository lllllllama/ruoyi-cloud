package com.ruoyi.fund.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.fund.constant.FundAuditConstants;
import com.ruoyi.fund.domain.FundAllocationPlan;
import com.ruoyi.fund.domain.FundUsePlan;
import com.ruoyi.fund.service.FundPermissionService;
import com.ruoyi.fund.service.IFundResearchService;
import com.ruoyi.fund.util.FundSecurityUtils;

@Service
public class FundPermissionServiceImpl implements FundPermissionService
{
    @Autowired private IFundResearchService researchService;

    public void assertAdmin(String operation)
    {
        if (!FundSecurityUtils.isSystemAdmin()) throw new ServiceException("Only system administrators may " + operation);
    }

    public void assertGroupMember(Long groupId, Long userId) { researchService.assertGroupMember(groupId, userId); }
    public void assertGroupLeader(Long groupId, Long userId) { researchService.assertGroupLeader(groupId, userId); }

    public void assertCanAssignAllocation(FundAllocationPlan plan, Long userId)
    {
        if (!FundSecurityUtils.isSystemAdmin()
                && !researchService.isUnitManager(plan.getTopicId(), plan.getAllocationDeptId(), userId))
            throw new ServiceException("Only the configured allocation unit manager may assign a responsible user");
    }

    public void assertCanOperateAllocation(FundAllocationPlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return;
        if (plan.getResponsibleUserId() != null && !userId.equals(plan.getResponsibleUserId()))
            throw new ServiceException("Only the assigned responsible user may operate this allocation plan");
        if (plan.getResponsibleUserId() == null
                && !researchService.isGroupUnitMember(plan.getTopicId(), plan.getAllocationDeptId(), userId))
            throw new ServiceException("Only allocation unit members may operate an unassigned plan");
    }

    public void assertCanOperateUse(FundUsePlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return;
        if (plan.getResponsibleUserId() != null && !userId.equals(plan.getResponsibleUserId()))
            throw new ServiceException("Only the assigned responsible user may operate this use plan");
        if (plan.getResponsibleUserId() == null) researchService.assertGroupMember(plan.getTopicId(), userId);
    }

    public void assertOwnRecord(Long submitUserId, Long userId)
    {
        if (!FundSecurityUtils.isSystemAdmin() && !userId.equals(submitUserId))
            throw new ServiceException("Only the submitter may modify or delete this record");
    }

    public void assertCanAccessBusiness(Long groupId, String businessType, Long userId)
    {
        if (FundAuditConstants.USE_PLAN.equals(businessType) || FundAuditConstants.USE_RECORD.equals(businessType))
            assertGroupMember(groupId, userId);
        else if (!FundAuditConstants.ALLOCATION_PLAN.equals(businessType)
                && !FundAuditConstants.ALLOCATION_RECORD.equals(businessType))
            throw new ServiceException("Unsupported fund business type");
    }
}
