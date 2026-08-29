package com.ruoyi.research.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.system.api.RemoteFundSupportService;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class ResearchOrgServiceImpl implements ResearchOrgService
{
    @Autowired
    private RemoteFundSupportService remoteService;

    @Override
    public List<FundDeptOption> getDepts()
    {
        return requireData(remoteService.getDepts(SecurityConstants.INNER), "No active departments are available");
    }

    @Override
    public FundUserOption getUser(Long userId)
    {
        return requireData(remoteService.getUser(userId, SecurityConstants.INNER), "User does not exist");
    }

    @Override
    public FundDeptOption getDept(Long deptId)
    {
        return requireData(remoteService.getDept(deptId, SecurityConstants.INNER), "Department does not exist");
    }

    private <T> T requireData(R<T> response, String notFoundMessage)
    {
        if (response == null || response.getCode() != R.SUCCESS)
        {
            throw new ServiceException(response == null ? "System service is unavailable" : response.getMsg());
        }
        if (response.getData() == null)
        {
            throw new ServiceException(notFoundMessage);
        }
        return response.getData();
    }
}
