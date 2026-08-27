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
import com.ruoyi.fund.domain.FundProjectBudget;
import com.ruoyi.fund.domain.FundUsePlan;
import com.ruoyi.fund.domain.FundUseRecord;
import com.ruoyi.fund.domain.dto.FundFinishRequest;
import com.ruoyi.fund.domain.vo.FundFinishCheckVo;
import com.ruoyi.fund.mapper.FundProjectBudgetMapper;
import com.ruoyi.fund.mapper.FundUsePlanMapper;
import com.ruoyi.fund.mapper.FundUseRecordMapper;
import com.ruoyi.fund.service.FundPermissionService;
import com.ruoyi.fund.service.IFundAttachmentService;
import com.ruoyi.fund.service.IFundOperationLogService;
import com.ruoyi.fund.service.IFundOrgService;
import com.ruoyi.fund.service.IFundResearchService;

@RunWith(MockitoJUnitRunner.class)
public class FundUseServiceImplTest
{
    @InjectMocks private FundUseServiceImpl service;
    @Mock private FundUsePlanMapper planMapper;
    @Mock private FundUseRecordMapper recordMapper;
    @Mock private FundProjectBudgetMapper budgetMapper;
    @Mock private IFundResearchService researchService;
    @Mock private IFundOrgService org;
    @Mock private IFundOperationLogService audit;
    @Mock private IFundAttachmentService attachmentService;
    @Mock private FundPermissionService permissionService;

    private final AtomicLong ids = new AtomicLong(200);

    @Before
    public void setUp()
    {
        login(1L);
        FundProjectBudget budget = new FundProjectBudget();
        budget.setBudgetId(1L);
        budget.setTopicId(1L);
        budget.setTotalAmount(money("100"));
        when(budgetMapper.selectByTopicIdForUpdate(1L)).thenReturn(budget);
        when(planMapper.insert(any(FundUsePlan.class))).thenAnswer(invocation -> {
            ((FundUsePlan) invocation.getArgument(0)).setUsePlanId(ids.incrementAndGet());
            return 1;
        });
    }

    @After public void tearDown() { SecurityContextHolder.remove(); }

    @Test
    public void usePlans60Plus40SucceedAnd60Plus50Fail()
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
        FundUsePlan plan = runningPlan();
        when(planMapper.selectById(10L)).thenReturn(plan);
        assertFinish("80", FundConstants.FINISH_UNDER, true);
        assertFinish("100", FundConstants.FINISH_NORMAL, false);
        assertFinish("120", FundConstants.FINISH_OVER, true);
    }

    @Test
    public void responsibleMemberAndOutsiderPermissionBranchesAreEnforced()
    {
        FundUsePlan plan = runningPlan();
        plan.setResponsibleUserId(10L);
        when(planMapper.selectById(10L)).thenReturn(plan);
        login(10L);
        service.finishCheck(10L);
        login(11L);
        doThrow(new ServiceException("无计划操作权限"))
                .when(permissionService).assertCanOperateUse(plan, 11L);
        assertDenied(() -> service.finishCheck(10L));

        plan.setResponsibleUserId(null);
        login(12L);
        service.finishCheck(10L);
        verify(permissionService).assertCanOperateUse(plan, 12L);
        login(13L);
        doThrow(new ServiceException("无课题访问权限"))
                .when(permissionService).assertCanOperateUse(plan, 13L);
        assertDenied(() -> service.finishCheck(10L));
    }

    @Test
    public void insertRecordAndFinishUseTheSamePlanRowLock()
    {
        FundUsePlan plan = runningPlan();
        when(planMapper.selectForUpdate(10L)).thenReturn(plan);
        when(recordMapper.insert(any(FundUseRecord.class))).thenAnswer(invocation -> {
            ((FundUseRecord) invocation.getArgument(0)).setUseRecordId(20L);
            return 1;
        });
        when(recordMapper.sumByPlanId(10L)).thenReturn(money("100"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("100"));

        FundUseRecord record = new FundUseRecord();
        record.setUsePlanId(10L);
        record.setAmount(money("20"));
        service.insertRecord(record);
        service.finish(10L, new FundFinishRequest());

        verify(planMapper, times(2)).selectForUpdate(10L);
        verify(planMapper).finish(eq(10L), eq(money("100")),
                argThat(value -> value.compareTo(BigDecimal.ZERO) == 0),
                eq(FundConstants.FINISH_NORMAL), isNull(), eq(false), eq(1L), isNull(), anyString());
    }

    @Test
    public void userFromAnotherResearchGroupCannotReadPlanOrModifyUseRecordById()
    {
        FundUsePlan foreignPlan = runningPlan();
        foreignPlan.setUsePlanId(99L);
        foreignPlan.setTopicId(2L);
        when(planMapper.selectById(99L)).thenReturn(foreignPlan);
        when(planMapper.selectForUpdate(99L)).thenReturn(foreignPlan);
        doThrow(new ServiceException("无课题访问权限"))
                .when(permissionService).assertGroupMember(2L, 10L);
        login(10L);
        assertDenied(() -> service.selectPlan(99L));

        FundUseRecord stored = new FundUseRecord();
        stored.setUseRecordId(88L);
        stored.setUsePlanId(99L);
        stored.setSubmitUserId(10L);
        when(recordMapper.selectById(88L)).thenReturn(stored);
        FundUseRecord update = new FundUseRecord();
        update.setUseRecordId(88L);
        update.setAmount(money("10"));
        assertDenied(() -> service.updateRecord(update));
        verify(recordMapper, never()).update(any(FundUseRecord.class));
    }

    private void assertFinish(String actual, String type, boolean confirm)
    {
        when(recordMapper.sumByPlanId(10L)).thenReturn(money(actual));
        FundFinishCheckVo result = service.finishCheck(10L);
        assertEquals(type, result.getFinishType());
        assertEquals(confirm, result.isNeedConfirm());
    }

    private FundUsePlan plan(BigDecimal amount)
    {
        FundUsePlan plan = runningPlan();
        plan.setUsePlanId(null);
        plan.setPlanAmount(amount);
        return plan;
    }

    private FundUsePlan runningPlan()
    {
        FundUsePlan plan = new FundUsePlan();
        plan.setUsePlanId(10L);
        plan.setTopicId(1L);
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
