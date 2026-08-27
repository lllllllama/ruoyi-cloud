package com.ruoyi.research.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.research.api.domain.ResearchGroupDto;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.api.domain.ResearchUserPermissionDto;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.ResearchRemoteQueryService;

@RestController
@RequestMapping("/internal/research")
public class RemoteResearchController
{
    @Autowired
    private ResearchPermissionService permissionService;

    @Autowired
    private ResearchRemoteQueryService remoteQueryService;

    @InnerAuth
    @GetMapping("/group/{groupId}")
    public R<ResearchGroupDto> getGroup(@PathVariable("groupId") Long groupId)
    {
        return R.ok(remoteQueryService.getGroup(groupId));
    }

    @InnerAuth
    @GetMapping("/group/{groupId}/permission/{userId}")
    public R<ResearchUserPermissionDto> getUserPermission(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId)
    {
        return R.ok(remoteQueryService.getUserPermission(groupId, userId));
    }

    @InnerAuth
    @GetMapping("/group/{groupId}/member/{userId}")
    public R<Boolean> isGroupMember(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId)
    {
        return R.ok(permissionService.isGroupMember(groupId, userId));
    }

    @InnerAuth
    @GetMapping("/group/{groupId}/leader/{userId}")
    public R<Boolean> isGroupLeader(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId)
    {
        return R.ok(permissionService.isGroupLeader(groupId, userId));
    }

    @InnerAuth
    @GetMapping("/group/{groupId}/unit/{deptId}/manager/{userId}")
    public R<Boolean> isUnitManager(@PathVariable("groupId") Long groupId,
            @PathVariable("deptId") Long deptId, @PathVariable("userId") Long userId)
    {
        return R.ok(permissionService.isUnitManager(groupId, deptId, userId));
    }

    @InnerAuth
    @GetMapping("/group/{groupId}/unit/{deptId}/member/{userId}")
    public R<Boolean> isGroupUnitMember(@PathVariable("groupId") Long groupId,
            @PathVariable("deptId") Long deptId, @PathVariable("userId") Long userId)
    {
        return R.ok(permissionService.isGroupUnitMember(groupId, deptId, userId));
    }

    @InnerAuth
    @GetMapping("/user/{userId}/groups")
    public R<List<Long>> getAllowedGroupIds(@PathVariable("userId") Long userId)
    {
        return R.ok(permissionService.getAllowedGroupIds(userId));
    }

    @InnerAuth
    @GetMapping("/group/{groupId}/unit/{deptId}/members")
    public R<List<ResearchGroupMemberDto>> getSelectableMembers(@PathVariable("groupId") Long groupId,
            @PathVariable("deptId") Long deptId)
    {
        return R.ok(remoteQueryService.getSelectableMembers(groupId, deptId));
    }
}
