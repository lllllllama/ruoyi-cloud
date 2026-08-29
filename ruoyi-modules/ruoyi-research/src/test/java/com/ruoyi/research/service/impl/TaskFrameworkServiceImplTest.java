package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.ResearchGroup;
import com.ruoyi.research.domain.ResearchGroupUnit;
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.vo.TaskFrameworkGroupOptionVo;
import com.ruoyi.research.mapper.ResearchGroupMapper;
import com.ruoyi.research.mapper.ResearchGroupUnitMapper;
import com.ruoyi.research.mapper.TaskFrameworkMapper;
import com.ruoyi.research.mapper.TaskFrameworkUnitMapper;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.system.api.domain.FundDeptOption;

@RunWith(MockitoJUnitRunner.class)
public class TaskFrameworkServiceImplTest
{
    @InjectMocks private TaskFrameworkServiceImpl service;
    @Mock private TaskFrameworkMapper frameworkMapper;
    @Mock private TaskFrameworkUnitMapper unitMapper;
    @Mock private ResearchGroupMapper groupMapper;
    @Mock private ResearchGroupUnitMapper groupUnitMapper;
    @Mock private TaskPermissionService permissionService;
    @Mock private ResearchOrgService orgService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("10");
        SecurityContextHolder.setUserName("leader");
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void annualFrameworkListUsesOnlyLeaderManagedGroups()
    {
        TaskFramework query = new TaskFramework();
        List<Long> managedGroupIds = Collections.singletonList(100L);
        when(permissionService.getManagedGroupIds(10L)).thenReturn(managedGroupIds);
        when(frameworkMapper.selectList(query, managedGroupIds)).thenReturn(Collections.emptyList());

        assertTrue(service.selectList(query).isEmpty());
        verify(permissionService).getManagedGroupIds(10L);
        verify(permissionService, never()).getAllowedGroupIds(10L);
    }

    @Test
    public void taskTreeOptionsRemainAvailableForAllGroupMembers()
    {
        List<Long> allowedGroupIds = Arrays.asList(100L, 200L);
        TaskFramework framework = new TaskFramework();
        framework.setFrameworkId(1L);
        when(permissionService.getAllowedGroupIds(10L)).thenReturn(allowedGroupIds);
        when(frameworkMapper.selectList(any(TaskFramework.class), eq(allowedGroupIds)))
                .thenReturn(Collections.singletonList(framework));

        assertEquals(Collections.singletonList(framework), service.selectOptions());
        verify(permissionService).getAllowedGroupIds(10L);
    }

    @Test
    public void nonAdministratorCannotCreateAnnualFramework()
    {
        doThrow(new ServiceException("denied")).when(permissionService).assertCanMaintainFramework(10L);

        try
        {
            service.insert(new TaskFramework());
            fail("Expected permission rejection");
        }
        catch (ServiceException expected)
        {
            assertEquals("denied", expected.getMessage());
        }
        verify(frameworkMapper, never()).insert(any(TaskFramework.class));
    }

    @Test
    public void managedGroupOptionsContainOnlyActiveUnitsWithBusinessNames()
    {
        ResearchGroup group = new ResearchGroup();
        group.setGroupId(100L);
        group.setGroupName("Group A");
        group.setStatus("0");
        ResearchGroupUnit active = unit(100L, 1000L, "LEAD", "0");
        ResearchGroupUnit disabled = unit(100L, 2000L, "PARTICIPANT", "1");
        FundDeptOption dept = new FundDeptOption();
        dept.setDeptId(1000L);
        dept.setDeptName("Research Department");

        when(permissionService.getManagedGroupIds(10L)).thenReturn(Collections.singletonList(100L));
        when(groupMapper.selectResearchGroupById(100L)).thenReturn(group);
        when(groupUnitMapper.selectByGroupId(100L)).thenReturn(Arrays.asList(active, disabled));
        when(orgService.getDepts()).thenReturn(Collections.singletonList(dept));

        List<TaskFrameworkGroupOptionVo> options = service.selectManagedGroupOptions();

        assertEquals(1, options.size());
        assertEquals(Long.valueOf(100L), options.get(0).getGroupId());
        assertEquals("Group A", options.get(0).getGroupName());
        assertEquals(1, options.get(0).getUnits().size());
        assertEquals("Research Department", options.get(0).getUnits().get(0).getDeptName());
    }

    private ResearchGroupUnit unit(Long groupId, Long deptId, String unitType, String status)
    {
        ResearchGroupUnit unit = new ResearchGroupUnit();
        unit.setGroupId(groupId);
        unit.setDeptId(deptId);
        unit.setUnitType(unitType);
        unit.setStatus(status);
        return unit;
    }
}
