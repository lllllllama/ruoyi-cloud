package com.ruoyi.research.service;

import java.util.List;

public interface ResearchPermissionService
{
    boolean isGroupMember(Long groupId, Long userId);

    boolean isGroupLeader(Long groupId, Long userId);

    boolean isGroupCore(Long groupId, Long userId);

    boolean isGroupExpert(Long groupId, Long userId);

    boolean isGroupUnitMember(Long groupId, Long deptId, Long userId);

    boolean isUnitManager(Long groupId, Long deptId, Long userId);

    boolean canViewGroup(Long groupId, Long userId);

    List<Long> getAllowedGroupIds(Long userId);
}
