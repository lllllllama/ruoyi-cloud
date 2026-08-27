package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.api.domain.ResearchGroupDto;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.api.domain.ResearchGroupUnitDto;
import com.ruoyi.research.api.domain.ResearchUserPermissionDto;
import com.ruoyi.research.domain.ResearchGroup;
import com.ruoyi.research.domain.ResearchGroupMember;
import com.ruoyi.research.domain.ResearchGroupUnit;
import com.ruoyi.research.mapper.ResearchGroupMemberMapper;
import com.ruoyi.research.service.ResearchGroupService;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.ResearchRemoteQueryService;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class ResearchRemoteQueryServiceImpl implements ResearchRemoteQueryService
{
    @Autowired
    private ResearchGroupService groupService;

    @Autowired
    private ResearchGroupMemberMapper memberMapper;

    @Autowired
    private ResearchPermissionService permissionService;

    @Autowired
    private ResearchOrgService orgService;

    @Override
    public ResearchGroupDto getGroup(Long groupId)
    {
        ResearchGroup group = groupService.selectById(groupId);
        if (group == null)
        {
            throw new ServiceException("Research group does not exist");
        }
        ResearchGroupDto dto = new ResearchGroupDto();
        dto.setGroupId(group.getGroupId());
        dto.setGroupCode(group.getGroupCode());
        dto.setGroupName(group.getGroupName());
        dto.setLeadDeptId(group.getLeadDeptId());
        dto.setDescription(group.getDescription());
        dto.setStatus(group.getStatus());
        dto.setUnits(toUnitDtos(group.getUnits()));
        dto.setMembers(toMemberDtos(memberMapper.selectByGroupId(groupId)));
        return dto;
    }

    @Override
    public ResearchUserPermissionDto getUserPermission(Long groupId, Long userId)
    {
        ResearchUserPermissionDto dto = new ResearchUserPermissionDto();
        dto.setGroupId(groupId);
        dto.setUserId(userId);
        dto.setGroupMember(permissionService.isGroupMember(groupId, userId));
        dto.setGroupLeader(permissionService.isGroupLeader(groupId, userId));
        dto.setGroupCore(permissionService.isGroupCore(groupId, userId));
        dto.setGroupExpert(permissionService.isGroupExpert(groupId, userId));
        dto.setCanView(permissionService.canViewGroup(groupId, userId));
        return dto;
    }

    @Override
    public List<ResearchGroupMemberDto> getSelectableMembers(Long groupId, Long deptId)
    {
        if (groupService.selectById(groupId) == null)
        {
            throw new ServiceException("Research group does not exist");
        }
        return toMemberDtos(memberMapper.selectActiveByGroupAndDept(groupId, deptId));
    }

    private List<ResearchGroupUnitDto> toUnitDtos(List<ResearchGroupUnit> units)
    {
        List<ResearchGroupUnitDto> result = new ArrayList<>();
        if (units == null)
        {
            return result;
        }
        for (ResearchGroupUnit unit : units)
        {
            ResearchGroupUnitDto dto = new ResearchGroupUnitDto();
            dto.setId(unit.getId());
            dto.setGroupId(unit.getGroupId());
            dto.setDeptId(unit.getDeptId());
            FundDeptOption dept = orgService.getDept(unit.getDeptId());
            dto.setDeptName(dept.getDeptName());
            dto.setUnitType(unit.getUnitType());
            dto.setManagerUserId(unit.getManagerUserId());
            if (unit.getManagerUserId() != null)
            {
                FundUserOption manager = orgService.getUser(unit.getManagerUserId());
                dto.setManagerUserName(displayName(manager));
            }
            dto.setStatus(unit.getStatus());
            result.add(dto);
        }
        return result;
    }

    private List<ResearchGroupMemberDto> toMemberDtos(List<ResearchGroupMember> members)
    {
        List<ResearchGroupMemberDto> result = new ArrayList<>();
        if (members == null)
        {
            return result;
        }
        for (ResearchGroupMember member : members)
        {
            ResearchGroupMemberDto dto = new ResearchGroupMemberDto();
            dto.setId(member.getId());
            dto.setGroupId(member.getGroupId());
            dto.setUserId(member.getUserId());
            FundUserOption user = orgService.getUser(member.getUserId());
            dto.setUserName(user.getUserName());
            dto.setNickName(user.getNickName());
            dto.setDeptId(member.getDeptId());
            FundDeptOption dept = orgService.getDept(member.getDeptId());
            dto.setDeptName(dept.getDeptName());
            dto.setMemberRole(member.getMemberRole());
            dto.setStatus(member.getStatus());
            dto.setJoinTime(member.getJoinTime());
            dto.setLeaveTime(member.getLeaveTime());
            result.add(dto);
        }
        return result;
    }

    private String displayName(FundUserOption user)
    {
        return user.getNickName() == null ? user.getUserName() : user.getNickName();
    }
}
