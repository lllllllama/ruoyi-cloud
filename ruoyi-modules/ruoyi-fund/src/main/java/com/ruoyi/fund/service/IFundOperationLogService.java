package com.ruoyi.fund.service;

import java.util.List;
import com.ruoyi.fund.domain.FundOperationLog;

public interface IFundOperationLogService
{
    void record(Long groupId, String businessType, Long businessId, String operationType,
            Object beforeData, Object afterData, String reason);

    List<FundOperationLog> selectList(FundOperationLog query);

    List<FundOperationLog> selectAuthorizedList(FundOperationLog query);
}
