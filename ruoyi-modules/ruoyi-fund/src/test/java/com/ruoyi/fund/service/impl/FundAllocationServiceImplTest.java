package com.ruoyi.fund.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.fund.constant.FundConstants;
import com.ruoyi.fund.domain.FundAllocationPlan;
import com.ruoyi.fund.domain.FundAllocationRecord;
import com.ruoyi.fund.domain.FundProjectBudget;
import com.ruoyi.fund.domain.dto.FundFinishRequest;
import com.ruoyi.fund.domain.vo.FundFinishCheckVo;
import com.ruoyi.fund.mapper.FundAllocationPlanMapper;
import com.ruoyi.fund.mapper.FundAllocationRecordMapper;
import com.ruoyi.fund.mapper.FundProjectBudgetMapper;
import com.ruoyi.fund.service.IFundAttachmentService;
import com.ruoyi.fund.service.IFundOperationLogService;
import com.ruoyi.fund.service.IFundOrgService;
import com.ruoyi.fund.service.IFundResearchService;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

@RunWith(MockitoJUnitRunner.class)
public class FundAllocationServiceImplTest
{
    @InjectMocks private FundAllocationServiceImpl service;
    @Mock private FundAllocationPlanMapper planMapper;
    @Mock private FundAllocationRecordMapper recordMapper;
    @Mock private FundProjectBudgetMapper budgetMapper;
    @Mock private IFundOrgService org;
    @Mock private IFundResearchService researchService;
    @Mock private IFundOperationLogService audit;
    @Mock private IFundAttachmentService attachmentService;

    private final AtomicLong ids = new AtomicLong(100);

    @Before
    public void setUp()
    {
        login(1L);
        FundProjectBudget budget = new FundProjectBudget();
        budget.setBudgetId(1L);
        budget.setTopicId(1L);
        budget.setTotalAmount(money("100"));
        when(budgetMapper.selectByTopicIdForUpdate(1L)).thenReturn(budget);
        when(researchService.isGroupUnit(eq(1L), anyLong())).thenReturn(true);
        FundDeptOption dept = new FundDeptOption();
        dept.setDeptName("测试单位");
        when(org.getDept(anyLong())).thenReturn(dept);
        when(planMapper.insert(any(FundAllocationPlan.class))).thenAnswer(invocation -> {
            ((FundAllocationPlan) invocation.getArgument(0)).setPlanId(ids.incrementAndGet());
            return 1;
        });
    }

    @After public void tearDown() { SecurityContextHolder.remove(); }

    @Test
    public void allocationPlans60Plus40SucceedAnd60Plus50Fail()
    {
        when(planMapper.sumPlanAmount(1L, null)).thenReturn(money("60"));
        assertEquals(1, service.insertPlan(plan(money("40"))));
        try
        {
            service.insertPlan(plan(money("50")));
            fail("60 + 50 must be rejected");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("不能超过"));
        }
    }

    @Test
    public void finishCheckCoversUnderExactAndOver()
    {
        FundAllocationPlan plan = runningPlan();
        when(planMapper.selectById(10L)).thenReturn(plan);
        assertFinish("80", FundConstants.FINISH_UNDER, true);
        assertFinish("100", FundConstants.FINISH_NORMAL, false);
        assertFinish("120", FundConstants.FINISH_OVER, true);
    }

    @Test
    public void responsibleAndUnitMemberPermissionBranchesAreEnforced()
    {
        FundAllocationPlan plan = runningPlan();
        plan.setResponsibleUserId(10L);
        when(planMapper.selectById(10L)).thenReturn(plan);
        login(10L);
        service.finishCheck(10L);
        login(11L);
        assertDenied(() -> service.finishCheck(10L));

        plan.setResponsibleUserId(null);
        when(researchService.isGroupUnitMember(1L, 2L, 12L)).thenReturn(true);
        login(12L);
        service.finishCheck(10L);
        login(13L);
        assertDenied(() -> service.finishCheck(10L));
    }

    @Test
    public void insertRecordAndFinishUseTheSamePlanRowLock()
    {
        FundAllocationPlan plan = runningPlan();
        when(planMapper.selectForUpdate(10L)).thenReturn(plan);
        when(recordMapper.insert(any(FundAllocationRecord.class))).thenAnswer(invocation -> {
            ((FundAllocationRecord) invocation.getArgument(0)).setRecordId(20L);
            return 1;
        });
        when(recordMapper.sumByPlanId(10L)).thenReturn(money("100"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("100"));

        FundAllocationRecord record = new FundAllocationRecord();
        record.setPlanId(10L);
        record.setAmount(money("20"));
        service.insertRecord(record);
        service.finish(10L, new FundFinishRequest());

        verify(planMapper, times(2)).selectForUpdate(10L);
        verify(planMapper).finish(eq(10L), eq(money("100")), argThat(value -> value.compareTo(BigDecimal.ZERO) == 0),
                eq(FundConstants.FINISH_NORMAL), isNull(), eq(1L), anyString());
    }

    @Test
    public void configuredUnitManagerCanAssignAGroupUnitMember()
    {
        FundAllocationPlan plan = runningPlan();
        when(planMapper.selectForUpdate(10L)).thenReturn(plan);
        when(researchService.isUnitManager(1L, 2L, 20L)).thenReturn(true);
        when(researchService.isGroupUnitMember(1L, 2L, 21L)).thenReturn(true);
        FundUserOption user = new FundUserOption();
        user.setUserName("responsible");
        user.setNickName("Responsible User");
        when(org.getUser(21L)).thenReturn(user);
        when(planMapper.assign(10L, 21L, "Responsible User", "user20")).thenReturn(1);

        login(20L);
        assertEquals(1, service.assign(10L, 21L));

        verify(researchService).isUnitManager(1L, 2L, 20L);
        verify(researchService).isGroupUnitMember(1L, 2L, 21L);
    }

    private void assertFinish(String actual, String type, boolean confirm)
    {
        when(recordMapper.sumByPlanId(10L)).thenReturn(money(actual));
        FundFinishCheckVo result = service.finishCheck(10L);
        assertEquals(type, result.getFinishType());
        assertEquals(confirm, result.isNeedConfirm());
    }

    private FundAllocationPlan plan(BigDecimal amount)
    {
        FundAllocationPlan plan = runningPlan();
        plan.setPlanId(null);
        plan.setPlanAmount(amount);
        plan.setReceiveDeptId(3L);
        return plan;
    }

    private FundAllocationPlan runningPlan()
    {
        FundAllocationPlan plan = new FundAllocationPlan();
        plan.setPlanId(10L);
        plan.setTopicId(1L);
        plan.setAllocationDeptId(2L);
        plan.setReceiveDeptId(3L);
        plan.setPlanAmount(money("100"));
        plan.setStatus(FundConstants.STATUS_RUNNING);
        return plan;
    }

    private void login(Long userId)
    {
        SecurityContextHolder.remove();
        SecurityContextHolder.setUserId(String.valueOf(userId));
        SecurityContextHolder.setUserName("user" + userId);
    }

    private void assertDenied(Runnable action)
    {
        try { action.run(); fail("permission must be denied"); }
        catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
