package com.ruoyi.research.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.research.mapper.ResearchPermissionMapper;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.util.ResearchSecurityUtils;

@Service
public class ResearchPermissionServiceImpl implements ResearchPermissionService
{
    private static final String ROLE_LEADER = "LEADER";
    private static final String ROLE_CORE = "CORE";
    private static final String ROLE_EXPERT = "EXPERT";

    @Autowired
    private ResearchPermissionMapper permissionMapper;

    @Override
    public boolean isGroupMember(Long groupId, Long userId)
    {
        return isAdmin(userId) || valid(groupId, userId) && permissionMapper.countActiveMember(groupId, userId) > 0;
    }

    @Override
    public boolean isGroupLeader(Long groupId, Long userId)
    {
        return hasRole(groupId, userId, ROLE_LEADER);
    }

    @Override
    public boolean isGroupCore(Long groupId, Long userId)
    {
        return hasRole(groupId, userId, ROLE_CORE);
    }

    @Override
    public boolean isGroupExpert(Long groupId, Long userId)
    {
        return hasRole(groupId, userId, ROLE_EXPERT);
    }

    @Override
    public boolean isGroupUnitMember(Long groupId, Long deptId, Long userId)
    {
        return isAdmin(userId) || valid(groupId, userId) && deptId != null
                && permissionMapper.countActiveUnitMember(groupId, deptId, userId) > 0;
    }

    @Override
    public boolean isUnitManager(Long groupId, Long deptId, Long userId)
    {
        return isAdmin(userId) || valid(groupId, userId) && deptId != null
                && permissionMapper.countActiveUnitManager(groupId, deptId, userId) > 0;
    }

    @Override
    public boolean canViewGroup(Long groupId, Long userId)
    {
        return isGroupMember(groupId, userId);
    }

    @Override
    public List<Long> getAllowedGroupIds(Long userId)
    {
        if (isAdmin(userId))
        {
            return permissionMapper.selectAllActiveGroupIds();
        }
        if (userId == null)
        {
            return Collections.emptyList();
        }
        return permissionMapper.selectAllowedGroupIds(userId);
    }

    private boolean hasRole(Long groupId, Long userId, String role)
    {
        return isAdmin(userId) || valid(groupId, userId)
                && permissionMapper.countActiveRole(groupId, userId, role) > 0;
    }

    private boolean valid(Long groupId, Long userId)
    {
        return groupId != null && userId != null;
    }

    private boolean isAdmin(Long userId)
    {
        return ResearchSecurityUtils.isSystemAdmin(userId);
    }
}
