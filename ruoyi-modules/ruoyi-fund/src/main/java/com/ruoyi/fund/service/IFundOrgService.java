package com.ruoyi.fund.service;
import java.util.List;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;
public interface IFundOrgService {
    List<FundDeptOption> getDepts(); FundDeptOption getDept(Long deptId);
    List<FundUserOption> getUsersByDept(Long deptId); FundUserOption getUser(Long userId);
}
