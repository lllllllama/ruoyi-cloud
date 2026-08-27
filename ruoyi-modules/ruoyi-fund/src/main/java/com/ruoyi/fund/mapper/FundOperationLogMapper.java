package com.ruoyi.fund.mapper;

import java.util.List;
import com.ruoyi.fund.domain.FundOperationLog;

public interface FundOperationLogMapper
{
    int insert(FundOperationLog log);

    List<FundOperationLog> selectList(FundOperationLog query);
}
