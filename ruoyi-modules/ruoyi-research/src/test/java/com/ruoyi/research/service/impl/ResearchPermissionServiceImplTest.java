package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.research.mapper.ResearchPermissionMapper;

@RunWith(MockitoJUnitRunner.class)
public class ResearchPermissionServiceImplTest
{
    private static final Long ADMIN = 1L;
    private static final Long GROUP_A = 100L;
    private static final Long GROUP_B = 200L;
    private static final Long DEPT_A = 1000L;
    private static final Long A_LEADER = 101L;
    private static final Long A_CORE = 102L;
    private static final Long A_MEMBER = 103L;
    private static final Long A_EXPERT = 104L;
    private static final Long B_LEADER = 201L;
    private static final Long B_CORE = 202L;
    private static final Long OUTSIDER = 999L;
    private static final Long ALLOCATION_UNIT_MANAGER = 301L;
    private static final Long ALLOCATION_RESPONSIBLE = 302L;

    @InjectMocks
    private ResearchPermissionServiceImpl service;

    @Mock
    private ResearchPermissionMapper mapper;

    @Before
    public void setUp()
    {
        member(GROUP_A, A_LEADER, A_CORE, A_MEMBER, A_EXPERT,
                ALLOCATION_UNIT_MANAGER, ALLOCATION_RESPONSIBLE);
        member(GROUP_B, B_LEADER, B_CORE);
        role(GROUP_A, A_LEADER, "LEADER");
        role(GROUP_A, A_CORE, "CORE");
        role(GROUP_A, A_EXPERT, "EXPERT");
        role(GROUP_B, B_LEADER, "LEADER");
        role(GROUP_B, B_CORE, "CORE");
        when(mapper.countActiveUnitManager(GROUP_A, DEPT_A, ALLOCATION_UNIT_MANAGER)).thenReturn(1);
        when(mapper.countActiveUnitMember(GROUP_A, DEPT_A, ALLOCATION_UNIT_MANAGER)).thenReturn(1);
        when(mapper.countActiveUnitMember(GROUP_A, DEPT_A, ALLOCATION_RESPONSIBLE)).thenReturn(1);
        when(mapper.selectAllowedGroupIds(OUTSIDER)).thenReturn(Collections.emptyList());
        when(mapper.selectAllowedGroupIds(A_LEADER)).thenReturn(Collections.singletonList(GROUP_A));
        when(mapper.selectAllowedGroupIds(B_CORE)).thenReturn(Collections.singletonList(GROUP_B));
        when(mapper.selectLeaderGroupIds(A_LEADER)).thenReturn(Collections.singletonList(GROUP_A));
        when(mapper.selectLeaderGroupIds(A_MEMBER)).thenReturn(Collections.emptyList());
        when(mapper.selectAllActiveGroupIds()).thenReturn(Arrays.asList(GROUP_A, GROUP_B));
    }

    @Test
    public void administratorCanAccessEveryResearchGroup()
    {
        assertTrue(service.isGroupLeader(GROUP_A, ADMIN));
        assertTrue(service.isGroupCore(GROUP_B, ADMIN));
        assertTrue(service.canViewGroup(GROUP_B, ADMIN));
        assertEquals(Arrays.asList(GROUP_A, GROUP_B), service.getAllowedGroupIds(ADMIN));
    }

    @Test
    public void groupARolesAreRecognizedWithoutLeakingIntoGroupB()
    {
        assertTrue(service.isGroupLeader(GROUP_A, A_LEADER));
        assertTrue(service.isGroupCore(GROUP_A, A_CORE));
        assertTrue(service.isGroupMember(GROUP_A, A_MEMBER));
        assertTrue(service.isGroupExpert(GROUP_A, A_EXPERT));
        assertFalse(service.isGroupLeader(GROUP_B, A_LEADER));
        assertFalse(service.canViewGroup(GROUP_B, A_MEMBER));
        assertEquals(Collections.singletonList(GROUP_A), service.getAllowedGroupIds(A_LEADER));
    }

    @Test
    public void annualFrameworkScopeContainsOnlyLedGroups()
    {
        assertEquals(Arrays.asList(GROUP_A, GROUP_B), service.getLeaderGroupIds(ADMIN));
        assertEquals(Collections.singletonList(GROUP_A), service.getLeaderGroupIds(A_LEADER));
        assertTrue(service.getLeaderGroupIds(A_MEMBER).isEmpty());
    }

    @Test
    public void groupBRolesAndOutsiderAreIsolatedFromGroupA()
    {
        assertTrue(service.isGroupLeader(GROUP_B, B_LEADER));
        assertTrue(service.isGroupCore(GROUP_B, B_CORE));
        assertFalse(service.canViewGroup(GROUP_A, B_CORE));
        assertEquals(Collections.singletonList(GROUP_B), service.getAllowedGroupIds(B_CORE));
        assertFalse(service.isGroupMember(GROUP_A, OUTSIDER));
        assertTrue(service.getAllowedGroupIds(OUTSIDER).isEmpty());
    }

    @Test
    public void allocationUnitManagerAndResponsibleHaveDifferentUnitPermissions()
    {
        assertTrue(service.isUnitManager(GROUP_A, DEPT_A, ALLOCATION_UNIT_MANAGER));
        assertTrue(service.isGroupUnitMember(GROUP_A, DEPT_A, ALLOCATION_UNIT_MANAGER));
        assertTrue(service.isGroupUnitMember(GROUP_A, DEPT_A, ALLOCATION_RESPONSIBLE));
        assertFalse(service.isUnitManager(GROUP_A, DEPT_A, ALLOCATION_RESPONSIBLE));
    }

    private void member(Long groupId, Long... userIds)
    {
        for (Long userId : userIds)
        {
            when(mapper.countActiveMember(groupId, userId)).thenReturn(1);
        }
    }

    private void role(Long groupId, Long userId, String role)
    {
        when(mapper.countActiveRole(groupId, userId, role)).thenReturn(1);
    }
}
