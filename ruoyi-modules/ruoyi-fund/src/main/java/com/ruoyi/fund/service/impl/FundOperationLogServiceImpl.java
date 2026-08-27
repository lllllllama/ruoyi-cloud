package com.ruoyi.fund.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.fund.constant.FundAuditConstants;
import com.ruoyi.fund.domain.FundOperationLog;
import com.ruoyi.fund.mapper.FundOperationLogMapper;
import com.ruoyi.fund.service.IFundResearchService;
import com.ruoyi.fund.service.IFundOperationLogService;
import com.ruoyi.fund.util.FundSecurityUtils;

@Service
public class FundOperationLogServiceImpl implements IFundOperationLogService
{
    @Autowired
    private FundOperationLogMapper mapper;

    @Autowired
    private IFundResearchService researchService;

    @Override
    public void record(Long groupId, String businessType, Long businessId, String operationType,
            Object beforeData, Object afterData, String reason)
    {
        FundOperationLog log = new FundOperationLog();
        log.setGroupId(groupId);
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setOperationType(operationType);
        log.setBeforeData(beforeData == null ? null : JSON.toJSONString(beforeData));
        log.setAfterData(afterData == null ? null : JSON.toJSONString(afterData));
        log.setReason(reason);
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperationTime(new Date());
        mapper.insert(log);
    }

    @Override
    public List<FundOperationLog> selectList(FundOperationLog query)
    {
        return mapper.selectList(query);
    }

    @Override
    public List<FundOperationLog> selectAuthorizedList(FundOperationLog query)
    {
        if (query == null || query.getGroupId() == null || query.getBusinessId() == null
                || query.getBusinessType() == null || query.getBusinessType().trim().isEmpty())
        {
            throw new ServiceException("课题、业务类型和业务ID不能为空");
        }
        String businessType = query.getBusinessType().trim();
        Long userId = SecurityUtils.getUserId();
        if ((FundAuditConstants.USE_PLAN.equals(businessType)
                || FundAuditConstants.USE_RECORD.equals(businessType))
                && !FundSecurityUtils.isSystemAdmin())
        {
            researchService.assertGroupMember(query.getGroupId(), userId);
        }
        else if (!FundAuditConstants.ALLOCATION_PLAN.equals(businessType)
                && !FundAuditConstants.ALLOCATION_RECORD.equals(businessType)
                && !FundAuditConstants.USE_PLAN.equals(businessType)
                && !FundAuditConstants.USE_RECORD.equals(businessType))
        {
            throw new ServiceException("不支持的资金业务类型");
        }
        query.setBusinessType(businessType);
        return mapper.selectList(query);
    }
}
