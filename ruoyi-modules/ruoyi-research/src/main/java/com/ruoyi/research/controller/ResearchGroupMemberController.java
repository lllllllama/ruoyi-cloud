package com.ruoyi.research.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.research.domain.ResearchGroupMember;
import com.ruoyi.research.service.ResearchGroupMemberService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.ResearchRemoteQueryService;

@RestController
@RequestMapping("/group/{groupId}/member")
public class ResearchGroupMemberController extends BaseController
{
    @Autowired
    private ResearchGroupMemberService memberService;

    @Autowired
    private ResearchPermissionService permissionService;

    @Autowired
    private ResearchRemoteQueryService remoteQueryService;

    @GetMapping("/options")
    public AjaxResult options(@PathVariable("groupId") Long groupId)
    {
        if (!permissionService.canViewGroup(groupId, SecurityUtils.getUserId()))
        {
            throw new ServiceException("No permission to view research group members");
        }
        return AjaxResult.success(remoteQueryService.getSelectableMembers(groupId, null));
    }

    @RequiresPermissions("research:group:list")
    @GetMapping("/list")
    public AjaxResult list(@PathVariable("groupId") Long groupId)
    {
        return AjaxResult.success(memberService.selectByGroupId(groupId));
    }

    @RequiresPermissions("research:group:edit")
    @Log(title = "Research group member", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@PathVariable("groupId") Long groupId,
            @Validated @RequestBody ResearchGroupMember member)
    {
        return toAjax(memberService.insert(groupId, member));
    }

    @RequiresPermissions("research:group:edit")
    @Log(title = "Research group member", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@PathVariable("groupId") Long groupId,
            @Validated @RequestBody ResearchGroupMember member)
    {
        return toAjax(memberService.update(groupId, member));
    }

    @RequiresPermissions("research:group:edit")
    @Log(title = "Research group member", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userId}")
    public AjaxResult remove(@PathVariable("groupId") Long groupId,
            @PathVariable("userId") Long userId)
    {
        return toAjax(memberService.delete(groupId, userId));
    }
}
