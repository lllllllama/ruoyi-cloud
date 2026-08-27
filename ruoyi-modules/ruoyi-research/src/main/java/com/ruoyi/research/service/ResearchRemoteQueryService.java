package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.api.domain.ResearchGroupDto;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.api.domain.ResearchUserPermissionDto;

public interface ResearchRemoteQueryService
{
    ResearchGroupDto getGroup(Long groupId);

    ResearchUserPermissionDto getUserPermission(Long groupId, Long userId);

    List<ResearchGroupMemberDto> getSelectableMembers(Long groupId, Long deptId);
}
