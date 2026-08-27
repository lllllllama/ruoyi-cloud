package com.ruoyi.fund.service.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.fund.constant.FundAuditConstants;
import com.ruoyi.fund.domain.FundAllocationPlan;
import com.ruoyi.fund.domain.FundUsePlan;
import com.ruoyi.fund.service.IFundResearchService;

@RunWith(MockitoJUnitRunner.class)
public class FundPermissionServiceImplTest
{
    private static final Long USER_ID = 10L;
    private static final Long GROUP_ID = 20L;
    private static final Long DEPT_ID = 30L;

    @InjectMocks
    private FundPermissionServiceImpl service;

    @Mock
    private IFundResearchService researchService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId(String.valueOf(USER_ID));
        SecurityContextHolder.setUserName("permission-user");
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void allocationAssignmentAllowsConfiguredUnitManagerOnly()
    {
        FundAllocationPlan plan = allocationPlan();
        when(researchService.isUnitManager(GROUP_ID, DEPT_ID, USER_ID)).thenReturn(true);
        service.assertCanAssignAllocation(plan, USER_ID);

        when(researchService.isUnitManager(GROUP_ID, DEPT_ID, USER_ID)).thenReturn(false);
        expectDenied(new Action()
        {
            public void run()
            {
                service.assertCanAssignAllocation(plan, USER_ID);
            }
        });
    }

    @Test
    public void assignedAllocationAndUsePlanAreRestrictedToResponsibleUser()
    {
        FundAllocationPlan allocation = allocationPlan();
        allocation.setResponsibleUserId(USER_ID);
        service.assertCanOperateAllocation(allocation, USER_ID);
        expectDenied(new Action()
        {
            public void run()
            {
                service.assertCanOperateAllocation(allocation, 11L);
            }
        });

        FundUsePlan use = new FundUsePlan();
        use.setTopicId(GROUP_ID);
        use.setResponsibleUserId(USER_ID);
        service.assertCanOperateUse(use, USER_ID);
        expectDenied(new Action()
        {
            public void run()
            {
                service.assertCanOperateUse(use, 11L);
            }
        });
    }

    @Test
    public void fundBusinessDataRequiresResearchGroupMembership()
    {
        service.assertCanAccessBusiness(GROUP_ID, FundAuditConstants.USE_RECORD, USER_ID);
        verify(researchService).assertGroupMember(GROUP_ID, USER_ID);

        service.assertCanAccessBusiness(GROUP_ID, FundAuditConstants.ALLOCATION_RECORD, USER_ID);
        verify(researchService, times(2)).assertGroupMember(GROUP_ID, USER_ID);
        verify(researchService, never()).assertGroupLeader(GROUP_ID, USER_ID);
    }

    @Test
    public void attachmentAccessAcceptsRecordTypesOnly()
    {
        service.assertCanDownloadAttachment(GROUP_ID, FundAuditConstants.USE_RECORD, USER_ID);
        verify(researchService).assertGroupMember(GROUP_ID, USER_ID);
        service.assertCanDownloadAttachment(GROUP_ID, FundAuditConstants.ALLOCATION_RECORD, USER_ID);
        verify(researchService, times(2)).assertGroupMember(GROUP_ID, USER_ID);

        expectDenied(new Action()
        {
            public void run()
            {
                service.assertCanDownloadAttachment(GROUP_ID, FundAuditConstants.USE_PLAN, USER_ID);
            }
        });
    }

    @Test
    public void groupLeaderCanConfirmForceFinish()
    {
        when(researchService.isGroupLeader(GROUP_ID, USER_ID)).thenReturn(true);
        assertTrue(service.canConfirmForceFinish(GROUP_ID, USER_ID));
    }

    @Test
    public void administratorBypassesRemoteMembershipChecks()
    {
        SecurityContextHolder.setUserId("1");
        service.assertCanAccessBusiness(GROUP_ID, FundAuditConstants.USE_RECORD, 1L);
        service.assertCanDownloadAttachment(GROUP_ID, FundAuditConstants.USE_RECORD, 1L);
        verify(researchService, never()).assertGroupMember(GROUP_ID, 1L);
    }

    private FundAllocationPlan allocationPlan()
    {
        FundAllocationPlan plan = new FundAllocationPlan();
        plan.setTopicId(GROUP_ID);
        plan.setAllocationDeptId(DEPT_ID);
        return plan;
    }

    private void expectDenied(Action action)
    {
        try
        {
            action.run();
            fail("Expected permission rejection");
        }
        catch (ServiceException expected)
        {
            // Expected.
        }
    }

    private interface Action
    {
        void run();
    }
}
