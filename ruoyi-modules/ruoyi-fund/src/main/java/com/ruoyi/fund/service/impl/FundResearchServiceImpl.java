package com.ruoyi.fund.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.fund.service.IFundResearchService;
import com.ruoyi.research.api.RemoteResearchService;
import com.ruoyi.research.api.domain.ResearchGroupDto;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.api.domain.ResearchGroupUnitDto;

@Service
public class FundResearchServiceImpl implements IFundResearchService
{
    @Autowired
    private RemoteResearchService remoteService;

    @Override
    public ResearchGroupDto getGroup(Long groupId)
    {
        ResearchGroupDto group = data(remoteService.getGroup(groupId, SecurityConstants.INNER));
        if (group == null)
        {
            throw new ServiceException("Research group does not exist");
        }
        return group;
    }

    @Override
    public List<Long> getAllowedGroupIds(Long userId)
    {
        List<Long> ids = data(remoteService.getAllowedGroupIds(userId, SecurityConstants.INNER));
        return ids == null ? Collections.<Long>emptyList() : ids;
    }

    @Override
    public boolean isGroupMember(Long groupId, Long userId)
    {
        return Boolean.TRUE.equals(data(remoteService.isGroupMember(groupId, userId, SecurityConstants.INNER)));
    }

    @Override
    public boolean isGroupLeader(Long groupId, Long userId)
    {
        return Boolean.TRUE.equals(data(remoteService.isGroupLeader(groupId, userId, SecurityConstants.INNER)));
    }

    @Override
    public boolean isGroupUnitMember(Long groupId, Long deptId, Long userId)
    {
        return Boolean.TRUE.equals(data(remoteService.isGroupUnitMember(
                groupId, deptId, userId, SecurityConstants.INNER)));
    }

    @Override
    public boolean isUnitManager(Long groupId, Long deptId, Long userId)
    {
        return Boolean.TRUE.equals(data(remoteService.isUnitManager(
                groupId, deptId, userId, SecurityConstants.INNER)));
    }

    @Override
    public boolean isGroupUnit(Long groupId, Long deptId)
    {
        ResearchGroupDto group = getGroup(groupId);
        if (group.getUnits() == null)
        {
            return false;
        }
        for (ResearchGroupUnitDto unit : group.getUnits())
        {
            if (deptId != null && deptId.equals(unit.getDeptId()) && "0".equals(unit.getStatus()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ResearchGroupMemberDto> getSelectableMembers(Long groupId, Long deptId)
    {
        List<ResearchGroupMemberDto> members = data(remoteService.getSelectableMembers(
                groupId, deptId, SecurityConstants.INNER));
        return members == null ? Collections.<ResearchGroupMemberDto>emptyList() : members;
    }

    @Override
    public void assertGroupMember(Long groupId, Long userId)
    {
        if (!isGroupMember(groupId, userId))
        {
            throw new ServiceException("No permission to access this research group's fund-use data");
        }
    }

    @Override
    public void assertGroupLeader(Long groupId, Long userId)
    {
        if (!isGroupLeader(groupId, userId))
        {
            throw new ServiceException("Only the research group leader may perform this operation");
        }
    }

    private <T> T data(R<T> response)
    {
        if (response == null || response.getCode() != R.SUCCESS)
        {
            throw new ServiceException(response == null ? "Research service is unavailable" : response.getMsg());
        }
        return response.getData();
    }
}
