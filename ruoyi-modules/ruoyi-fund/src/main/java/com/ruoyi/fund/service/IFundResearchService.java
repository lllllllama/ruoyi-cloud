package com.ruoyi.fund.service;

import java.util.List;
import com.ruoyi.research.api.domain.ResearchGroupDto;

public interface IFundResearchService
{
    ResearchGroupDto getGroup(Long groupId);

    List<Long> getAllowedGroupIds(Long userId);

    boolean isGroupMember(Long groupId, Long userId);

    boolean isGroupLeader(Long groupId, Long userId);

    boolean isGroupUnitMember(Long groupId, Long deptId, Long userId);

    boolean isUnitManager(Long groupId, Long deptId, Long userId);

    boolean isGroupUnit(Long groupId, Long deptId);

    void assertGroupMember(Long groupId, Long userId);

    void assertGroupLeader(Long groupId, Long userId);
}
