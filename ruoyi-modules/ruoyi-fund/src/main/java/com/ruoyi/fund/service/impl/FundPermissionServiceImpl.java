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
        if (!FundSecurityUtils.isSystemAdmin()
                && !researchService.isUnitManager(plan.getTopicId(), plan.getAllocationDeptId(), userId))
            throw new ServiceException("仅课题配置的拨付单位负责人可指定责任人");
    }

    public void assertCanOperateAllocation(FundAllocationPlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return;
        researchService.assertGroupMember(plan.getTopicId(), userId);
        if (plan.getResponsibleUserId() != null && !userId.equals(plan.getResponsibleUserId()))
            throw new ServiceException("该计划已指定责任人，仅责任人可操作");
        if (plan.getResponsibleUserId() == null
                && !researchService.isGroupUnitMember(plan.getTopicId(), plan.getAllocationDeptId(), userId))
            throw new ServiceException("仅当前课题拨付单位成员可操作未指定责任人的计划");
    }

    public void assertCanOperateUse(FundUsePlan plan, Long userId)
    {
        if (FundSecurityUtils.isSystemAdmin()) return;
        researchService.assertGroupMember(plan.getTopicId(), userId);
        if (plan.getResponsibleUserId() != null && !userId.equals(plan.getResponsibleUserId()))
            throw new ServiceException("该计划已指定责任人，仅责任人可操作");
        if (plan.getResponsibleUserId() == null) researchService.assertGroupMember(plan.getTopicId(), userId);
    }

    public boolean canConfirmForceFinish(Long groupId, Long userId)
    {
        return FundSecurityUtils.isSystemAdmin() || researchService.isGroupLeader(groupId, userId);
    }

    public void assertOwnRecord(Long submitUserId, Long userId)
    {
        if (!FundSecurityUtils.isSystemAdmin() && !userId.equals(submitUserId))
            throw new ServiceException("只能修改或删除本人提交的记录");
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
