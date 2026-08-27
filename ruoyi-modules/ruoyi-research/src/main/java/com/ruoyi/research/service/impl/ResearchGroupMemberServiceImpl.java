package com.ruoyi.research.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.ResearchGroupMember;
import com.ruoyi.research.mapper.ResearchGroupMapper;
import com.ruoyi.research.mapper.ResearchGroupMemberMapper;
import com.ruoyi.research.mapper.ResearchGroupUnitMapper;
import com.ruoyi.research.service.ResearchGroupMemberService;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.util.ResearchSecurityUtils;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class ResearchGroupMemberServiceImpl implements ResearchGroupMemberService
{
    private static final String STATUS_ACTIVE = "0";

    @Autowired
    private ResearchGroupMapper groupMapper;

    @Autowired
    private ResearchGroupUnitMapper unitMapper;

    @Autowired
    private ResearchGroupMemberMapper memberMapper;

    @Autowired
    private ResearchOrgService orgService;

    @Override
    public List<ResearchGroupMember> selectByGroupId(Long groupId)
    {
        requireGroup(groupId);
        return memberMapper.selectByGroupId(groupId);
    }

    @Override
    @Transactional
    public int insert(Long groupId, ResearchGroupMember member)
    {
        assertAdmin();
        member.setGroupId(groupId);
        validateMember(member, null);
        member.setStatus(StringUtils.isEmpty(member.getStatus()) ? STATUS_ACTIVE : member.getStatus());
        member.setCreateBy(SecurityUtils.getUsername());
        return memberMapper.insert(member);
    }

    @Override
    @Transactional
    public int update(Long groupId, ResearchGroupMember member)
    {
        assertAdmin();
        if (member.getId() == null)
        {
            throw new ServiceException("Membership ID is required");
        }
        ResearchGroupMember stored = memberMapper.selectById(member.getId());
        if (stored == null || !groupId.equals(stored.getGroupId()))
        {
            throw new ServiceException("Research group membership does not exist");
        }
        member.setGroupId(groupId);
        if (member.getJoinTime() == null)
        {
            member.setJoinTime(stored.getJoinTime());
        }
        validateMember(member, member.getId());
        member.setStatus(StringUtils.isEmpty(member.getStatus()) ? STATUS_ACTIVE : member.getStatus());
        return memberMapper.update(member);
    }

    @Override
    @Transactional
    public int delete(Long groupId, Long userId)
    {
        assertAdmin();
        requireGroup(groupId);
        return memberMapper.deleteByGroupAndUser(groupId, userId);
    }

    private void validateMember(ResearchGroupMember member, Long excludeId)
    {
        requireGroup(member.getGroupId());
        FundUserOption user = orgService.getUser(member.getUserId());
        orgService.getDept(member.getDeptId());
        if (user.getDeptId() == null || !member.getDeptId().equals(user.getDeptId()))
        {
            throw new ServiceException("Member department must match the user's current department");
        }
        if (unitMapper.countActiveByGroupAndDept(member.getGroupId(), member.getDeptId()) == 0)
        {
            throw new ServiceException("Member department must be a lead or participant unit of the research group");
        }
        if (memberMapper.countSameRole(member.getGroupId(), member.getUserId(), member.getMemberRole(), excludeId) > 0)
        {
            throw new ServiceException("The user already has this role in the research group");
        }
    }

    private void requireGroup(Long groupId)
    {
        if (groupId == null || groupMapper.selectResearchGroupById(groupId) == null)
        {
            throw new ServiceException("Research group does not exist");
        }
    }

    private void assertAdmin()
    {
        if (!ResearchSecurityUtils.isSystemAdmin())
        {
            throw new ServiceException("Only system administrators may maintain research group members");
        }
    }
}
