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
        if (!FundSecurityUtils.isSystemAdmin()) throw new ServiceException("仅系统管理员可" + operation);
    }

    public void assertGroupMember(Long groupId, Long userId)
    {
        if (!FundSecurityUtils.isSystemAdmin()) researchService.assertGroupMember(groupId, userId);
    }

    public void assertGroupLeader(Long groupId, Long userId)
    {
        if (!FundSecurityUtils.isSystemAdmin()) researchService.assertGroupLeader(groupId, userId);
    }

    public void assertCanAssignAllocation(FundAllocationPlan plan, Long userId)
    {
        if (!canAssignAllocation(plan, userId))
            throw new ServiceException("仅课题配置的拨付单位负责人可指定责任人");
    }

    public boolean canAssignAllocation(FundAllocationPlan plan, Long userId)
    {
        return FundSecurityUtils.isSystemAdmin()
                || researchService.isUnitManager(plan.getTopicId(), plan.getAllocationDeptId(), userId);
    }

    public void assertCanOperateAllocation(FundAllocationPlan plan, Long userId)
    {
        if (!canOperateAllocation(plan, userId))
            throw new ServiceException("仅当前责任人可操作；未指定责任人时由拨付单位负责人处理");
    }

    public boolean canOperateAllocation(FundAllocationPlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return true;
        if (!researchService.isGroupMember(plan.getTopicId(), userId)) return false;
        if (plan.getResponsibleUserId() != null)
            return userId.equals(plan.getResponsibleUserId());
        return researchService.isUnitManager(plan.getTopicId(), plan.getAllocationDeptId(), userId);
    }

    public void assertCanFinishAllocation(FundAllocationPlan plan, Long userId)
    {
        if (!canFinishAllocation(plan, userId))
            throw new ServiceException("仅当前责任人或拨付单位负责人可结束拨付计划");
    }

    public boolean canFinishAllocation(FundAllocationPlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return true;
        if (!researchService.isGroupMember(plan.getTopicId(), userId)) return false;
        return userId.equals(plan.getResponsibleUserId())
                || researchService.isUnitManager(plan.getTopicId(), plan.getAllocationDeptId(), userId);
    }

    public void assertCanOperateUse(FundUsePlan plan, Long userId)
    {
        if (!canOperateUse(plan, userId))
            throw new ServiceException("仅当前责任人可操作；历史未指定责任人的计划由课题负责人处理");
    }

    public boolean canOperateUse(FundUsePlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return true;
        if (!researchService.isGroupMember(plan.getTopicId(), userId)) return false;
        if (plan.getResponsibleUserId() != null)
            return userId.equals(plan.getResponsibleUserId());
        return researchService.isGroupLeader(plan.getTopicId(), userId);
    }

    public boolean canConfirmForceFinish(Long groupId, Long userId)
    {
        return FundSecurityUtils.isSystemAdmin() || researchService.isGroupLeader(groupId, userId);
    }

    public void assertOwnRecord(Long submitUserId, Long userId)
    {
        if (!canEditRecord(submitUserId, userId))
            throw new ServiceException("只能修改或删除本人提交的记录");
    }

    public boolean canEditRecord(Long submitUserId, Long userId)
    {
        return FundSecurityUtils.isSystemAdmin() || userId.equals(submitUserId);
    }

    public void assertCanAccessBusiness(Long groupId, String businessType, Long userId)
    {
        if (FundAuditConstants.ALLOCATION_PLAN.equals(businessType)
                || FundAuditConstants.ALLOCATION_RECORD.equals(businessType)
                || FundAuditConstants.USE_PLAN.equals(businessType)
                || FundAuditConstants.USE_RECORD.equals(businessType))
            assertGroupMember(groupId, userId);
        else
            throw new ServiceException("不支持的资金业务类型");
    }

    public void assertCanDownloadAttachment(Long groupId, String businessType, Long userId)
    {
        if (FundAuditConstants.ALLOCATION_RECORD.equals(businessType)
                || FundAuditConstants.USE_RECORD.equals(businessType))
            assertGroupMember(groupId, userId);
        else
            throw new ServiceException("不支持的附件业务类型");
    }
}
