package com.ruoyi.research.service;

import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

public interface ResearchOrgService
{
    FundUserOption getUser(Long userId);

    FundDeptOption getDept(Long deptId);
}
