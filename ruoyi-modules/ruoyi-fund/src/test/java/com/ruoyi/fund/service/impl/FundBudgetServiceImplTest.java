package com.ruoyi.fund.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.fund.domain.FundProjectBudget;
import com.ruoyi.fund.mapper.FundAllocationPlanMapper;
import com.ruoyi.fund.mapper.FundAllocationRecordMapper;
import com.ruoyi.fund.mapper.FundProjectBudgetMapper;
import com.ruoyi.fund.mapper.FundUsePlanMapper;
import com.ruoyi.fund.mapper.FundUseRecordMapper;
import com.ruoyi.fund.service.FundPermissionService;
import com.ruoyi.fund.service.IFundResearchService;

@RunWith(MockitoJUnitRunner.class)
public class FundBudgetServiceImplTest
{
    @InjectMocks private FundBudgetServiceImpl service;
    @Mock private FundProjectBudgetMapper mapper;
    @Mock private FundAllocationPlanMapper allocationPlanMapper;
    @Mock private FundAllocationRecordMapper allocationRecordMapper;
    @Mock private FundUsePlanMapper usePlanMapper;
    @Mock private FundUseRecordMapper useRecordMapper;
    @Mock private IFundResearchService researchService;
    @Mock private FundPermissionService permissionService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        FundProjectBudget old = budget("200");
        when(mapper.selectById(1L)).thenReturn(old);
        when(mapper.selectByTopicIdForUpdate(1L)).thenReturn(old);
    }

    @After public void tearDown() { SecurityContextHolder.remove(); }

    @Test
    public void budgetCannotBeReducedBelowPlannedAllocation()
    {
        when(allocationPlanMapper.sumPlanAmount(1L, null)).thenReturn(money("120"));
        assertDenied(() -> service.update(budget("100")));
    }

    @Test
    public void budgetCannotBeReducedBelowPlannedUse()
    {
        when(usePlanMapper.sumPlanAmount(1L, null)).thenReturn(money("120"));
        assertDenied(() -> service.update(budget("100")));
    }

    @Test
    public void budgetCannotBeReducedBelowActualAllocation()
    {
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("120"));
        assertDenied(() -> service.update(budget("100")));
    }

    @Test
    public void budgetCannotBeReducedBelowActualUse()
    {
        when(useRecordMapper.sumByTopicId(1L)).thenReturn(money("120"));
        assertDenied(() -> service.update(budget("100")));
    }

    private FundProjectBudget budget(String total)
    {
        FundProjectBudget budget = new FundProjectBudget();
        budget.setBudgetId(1L);
        budget.setTopicId(1L);
        budget.setTotalAmount(money(total));
        return budget;
    }

    private void assertDenied(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected budget reduction to be rejected");
        }
        catch (ServiceException expected)
        {
            assertNotNull(expected.getMessage());
        }
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
