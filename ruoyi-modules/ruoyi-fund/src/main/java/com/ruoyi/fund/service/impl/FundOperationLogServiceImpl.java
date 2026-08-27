package com.ruoyi.fund.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.fund.domain.FundOperationLog;
import com.ruoyi.fund.mapper.FundOperationLogMapper;
import com.ruoyi.fund.service.IFundOperationLogService;

@Service
public class FundOperationLogServiceImpl implements IFundOperationLogService
{
    @Autowired
    private FundOperationLogMapper mapper;

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
}
