package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

public interface ResearchOrgService
{
    List<FundDeptOption> getDepts();

    FundUserOption getUser(Long userId);

    FundDeptOption getDept(Long deptId);
}
