package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.domain.ResearchGroup;
import com.ruoyi.research.domain.ResearchGroupMember;
import com.ruoyi.research.mapper.ResearchGroupMemberMapper;
import com.ruoyi.research.service.ResearchGroupService;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

@RunWith(MockitoJUnitRunner.class)
public class ResearchRemoteQueryServiceImplTest
{
    @InjectMocks private ResearchRemoteQueryServiceImpl service;
    @Mock private ResearchGroupService groupService;
    @Mock private ResearchGroupMemberMapper memberMapper;
    @Mock private ResearchPermissionService permissionService;
    @Mock private ResearchOrgService orgService;

    @Before
    public void setUp()
    {
        when(groupService.selectById(1L)).thenReturn(new ResearchGroup());
        FundUserOption user = new FundUserOption();
        user.setUserName("member");
        user.setNickName("Member");
        when(orgService.getUser(10L)).thenReturn(user);
        FundDeptOption dept = new FundDeptOption();
        dept.setDeptName("Research Dept");
        when(orgService.getDept(100L)).thenReturn(dept);
    }

    @Test
    public void nullDepartmentReturnsAllActiveGroupMembers()
    {
        ResearchGroupMember active = member(1L, 10L, 100L, "0");
        ResearchGroupMember inactive = member(2L, 11L, 100L, "1");
        when(memberMapper.selectByGroupId(1L)).thenReturn(Arrays.asList(active, inactive));

        List<ResearchGroupMemberDto> result = service.getSelectableMembers(1L, null);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(10L), result.get(0).getUserId());
        verify(memberMapper).selectByGroupId(1L);
    }

    private ResearchGroupMember member(Long id, Long userId, Long deptId, String status)
    {
        ResearchGroupMember member = new ResearchGroupMember();
        member.setId(id);
        member.setGroupId(1L);
        member.setUserId(userId);
        member.setDeptId(deptId);
        member.setMemberRole("MEMBER");
        member.setStatus(status);
        return member;
    }
}
