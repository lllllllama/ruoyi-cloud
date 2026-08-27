package com.ruoyi.research.api;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.research.api.domain.ResearchGroupDto;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.api.domain.ResearchUserPermissionDto;
import com.ruoyi.research.api.factory.RemoteResearchFallbackFactory;

/**
 * Research service remote API.
 */
@FeignClient(contextId = "remoteResearchService", value = "ruoyi-research",
        fallbackFactory = RemoteResearchFallbackFactory.class)
public interface RemoteResearchService
{
    @GetMapping("/internal/research/group/{groupId}")
    R<ResearchGroupDto> getGroup(@PathVariable("groupId") Long groupId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/research/group/{groupId}/permission/{userId}")
    R<ResearchUserPermissionDto> getUserPermission(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/research/group/{groupId}/member/{userId}")
    R<Boolean> isGroupMember(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/research/group/{groupId}/leader/{userId}")
    R<Boolean> isGroupLeader(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/research/group/{groupId}/unit/{deptId}/manager/{userId}")
    R<Boolean> isUnitManager(@PathVariable("groupId") Long groupId,
            @PathVariable("deptId") Long deptId,
            @PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/research/user/{userId}/groups")
    R<List<Long>> getAllowedGroupIds(@PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/research/group/{groupId}/unit/{deptId}/members")
    R<List<ResearchGroupMemberDto>> getSelectableMembers(@PathVariable("groupId") Long groupId,
            @PathVariable("deptId") Long deptId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
