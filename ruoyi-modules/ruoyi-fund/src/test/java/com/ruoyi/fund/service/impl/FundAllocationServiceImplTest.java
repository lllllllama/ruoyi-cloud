package com.ruoyi.fund.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
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
import com.ruoyi.fund.mapper.FundUseRecordMapper;
import com.ruoyi.fund.service.FundPermissionService;
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
    @Mock private FundUseRecordMapper useRecordMapper;
    @Mock private IFundOrgService org;
    @Mock private IFundResearchService researchService;
    @Mock private IFundOperationLogService audit;
    @Mock private IFundAttachmentService attachmentService;
    @Mock private FundPermissionService permissionService;

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
        when(planMapper.selectById(10L)).thenReturn(runningPlan());
        when(recordMapper.selectForUpdate(anyLong())).thenAnswer(invocation ->
                recordMapper.selectById(invocation.getArgument(0)));
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
    public void insertAllocationWithinBudgetSucceeds()
    {
        FundAllocationPlan plan = runningPlan();
        when(planMapper.selectForUpdate(10L)).thenReturn(plan);
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("60"));
        when(recordMapper.insert(any(FundAllocationRecord.class))).thenAnswer(invocation -> {
            ((FundAllocationRecord) invocation.getArgument(0)).setRecordId(20L);
            return 1;
        });

        FundAllocationRecord record = allocationRecord(null, "40");
        assertEquals(1, service.insertRecord(record));

        verify(recordMapper).insert(record);
    }

    @Test
    public void insertAllocationExceedingBudgetFailsBeforeInsert()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("90"));

        assertDenied(() -> service.insertRecord(allocationRecord(null, "20")));

        verify(recordMapper, never()).insert(any(FundAllocationRecord.class));
        verify(attachmentService, never()).consume(anyLong(), anyString(), anyLong(), anyString());
        verify(audit, never()).record(anyLong(), anyString(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    public void updateAllocationRecordRecalculatesProjectActual()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        FundAllocationRecord old = allocationRecord(20L, "50");
        when(recordMapper.selectById(20L)).thenReturn(old);
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("95"));
        when(useRecordMapper.sumByTopicId(1L)).thenReturn(money("80"));
        when(recordMapper.update(any(FundAllocationRecord.class))).thenReturn(1);

        assertEquals(1, service.updateRecord(allocationRecord(20L, "40")));
    }

    @Test
    public void allocationRecordUpdateLocksBudgetThenPlanThenRecord()
    {
        FundAllocationPlan plan = runningPlan();
        FundAllocationRecord stored = allocationRecord(20L, "40");
        when(planMapper.selectById(10L)).thenReturn(plan);
        when(planMapper.selectForUpdate(10L)).thenReturn(plan);
        when(recordMapper.selectById(20L)).thenReturn(stored);
        when(recordMapper.selectForUpdate(20L)).thenReturn(stored);
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("40"));
        when(recordMapper.update(any(FundAllocationRecord.class))).thenReturn(1);

        service.updateRecord(allocationRecord(20L, "40"));

        InOrder locks = inOrder(budgetMapper, planMapper, recordMapper);
        locks.verify(budgetMapper).selectByTopicIdForUpdate(1L);
        locks.verify(planMapper).selectForUpdate(10L);
        locks.verify(recordMapper).selectForUpdate(20L);
    }

    @Test
    public void updateAllocationRecordCannotExceedBudget()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.selectById(20L)).thenReturn(allocationRecord(20L, "10"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("90"));

        assertDenied(() -> service.updateRecord(allocationRecord(20L, "30")));
        verify(recordMapper, never()).update(any(FundAllocationRecord.class));
    }

    @Test
    public void updateAllocationRecordCannotReduceAllocationBelowActualUse()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.selectById(20L)).thenReturn(allocationRecord(20L, "50"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("100"));
        when(useRecordMapper.sumByTopicId(1L)).thenReturn(money("80"));

        assertDenied(() -> service.updateRecord(allocationRecord(20L, "20")));
        verify(recordMapper, never()).update(any(FundAllocationRecord.class));
    }

    @Test
    public void deleteAllocationRecordCannotReduceAllocationBelowActualUse()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.selectById(20L)).thenReturn(allocationRecord(20L, "30"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("100"));
        when(useRecordMapper.sumByTopicId(1L)).thenReturn(money("80"));

        assertDenied(() -> service.deleteRecord(20L));
        verify(recordMapper, never()).deleteById(anyLong());
        verify(attachmentService, never()).deleteByBusiness(anyString(), anyLong());
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
    public void responsibleAndUnitManagerFinishPermissionBranchesAreEnforced()
    {
        FundAllocationPlan plan = runningPlan();
        plan.setResponsibleUserId(10L);
        when(planMapper.selectById(10L)).thenReturn(plan);
        login(10L);
        service.finishCheck(10L);
        login(11L);
        doThrow(new ServiceException("无计划操作权限"))
                .when(permissionService).assertCanFinishAllocation(plan, 11L);
        assertDenied(() -> service.finishCheck(10L));

        plan.setResponsibleUserId(null);
        login(12L);
        service.finishCheck(10L);
        login(13L);
        doThrow(new ServiceException("无计划操作权限"))
                .when(permissionService).assertCanFinishAllocation(plan, 13L);
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
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("0"), money("100"));

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
        when(researchService.isGroupUnitMember(1L, 2L, 21L)).thenReturn(true);
        FundUserOption user = new FundUserOption();
        user.setUserName("responsible");
        user.setNickName("Responsible User");
        when(org.getUser(21L)).thenReturn(user);
        when(planMapper.assign(10L, 21L, "Responsible User", "user20")).thenReturn(1);

        login(20L);
        assertEquals(1, service.assign(10L, 21L));

        verify(permissionService).assertCanAssignAllocation(plan, 20L);
        verify(researchService).isGroupUnitMember(1L, 2L, 21L);
    }

    @Test
    public void finishPersistsUnderExactAndOverExecutionResults()
    {
        FundProjectBudget budget = new FundProjectBudget();
        budget.setBudgetId(1L);
        budget.setTopicId(1L);
        budget.setTotalAmount(money("200"));
        when(budgetMapper.selectByTopicIdForUpdate(1L)).thenReturn(budget);
        FundAllocationPlan under = runningPlan();
        FundAllocationPlan exact = runningPlan();
        exact.setPlanId(11L);
        FundAllocationPlan over = runningPlan();
        over.setPlanId(12L);
        when(planMapper.selectForUpdate(10L)).thenReturn(under);
        when(planMapper.selectForUpdate(11L)).thenReturn(exact);
        when(planMapper.selectForUpdate(12L)).thenReturn(over);
        when(planMapper.selectById(10L)).thenReturn(under);
        when(planMapper.selectById(11L)).thenReturn(exact);
        when(planMapper.selectById(12L)).thenReturn(over);
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("120"));
        when(recordMapper.sumByPlanId(10L)).thenReturn(money("80"));
        when(recordMapper.sumByPlanId(11L)).thenReturn(money("100"));
        when(recordMapper.sumByPlanId(12L)).thenReturn(money("120"));

        service.finish(10L, confirmed("余额不再拨付"));
        service.finish(11L, new FundFinishRequest());
        service.finish(12L, confirmed("经确认允许单计划超额"));

        verify(planMapper).finish(10L, money("80"), money("-20"), FundConstants.FINISH_UNDER,
                "余额不再拨付", 1L, "user1");
        verify(planMapper).finish(11L, money("100"), money("0"), FundConstants.FINISH_NORMAL,
                null, 1L, "user1");
        verify(planMapper).finish(12L, money("120"), money("20"), FundConstants.FINISH_OVER,
                "经确认允许单计划超额", 1L, "user1");
    }

    @Test
    public void recordInsertWaitingBehindSuccessfulCloseIsRejected() throws Exception
    {
        FundAllocationPlan plan = runningPlan();
        CountDownLatch closeHasLock = new CountDownLatch(1);
        CountDownLatch closeCommitted = new CountDownLatch(1);
        when(planMapper.selectForUpdate(10L)).thenAnswer(invocation -> {
            if (Thread.currentThread().getName().contains("record-after-close"))
            {
                closeCommitted.await(5, TimeUnit.SECONDS);
            }
            else
            {
                closeHasLock.countDown();
            }
            return plan;
        });
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("100"));
        when(recordMapper.sumByPlanId(10L)).thenReturn(money("100"));
        AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        AtomicReference<Throwable> recordFailure = new AtomicReference<>();
        Thread closeThread = new Thread(() -> {
            try
            {
                service.finish(10L, new FundFinishRequest());
                plan.setStatus(FundConstants.STATUS_FINISHED);
            }
            catch (Throwable error)
            {
                closeFailure.set(error);
            }
            finally
            {
                closeCommitted.countDown();
            }
        }, "allocation-close");
        Thread recordThread = new Thread(() -> {
            try
            {
                FundAllocationRecord record = new FundAllocationRecord();
                record.setPlanId(10L);
                record.setAmount(money("1"));
                service.insertRecord(record);
            }
            catch (Throwable error)
            {
                recordFailure.set(error);
            }
        }, "allocation-record-after-close");

        closeThread.start();
        assertTrue(closeHasLock.await(5, TimeUnit.SECONDS));
        recordThread.start();
        closeThread.join(5000);
        recordThread.join(5000);

        assertFalse(closeThread.isAlive());
        assertFalse(recordThread.isAlive());
        assertNull(closeFailure.get());
        assertTrue(recordFailure.get() instanceof ServiceException);
        verify(recordMapper, never()).insert(any(FundAllocationRecord.class));
    }

    @Test
    public void userFromAnotherResearchGroupCannotReadPlanOrModifyAllocationRecordById()
    {
        FundAllocationPlan foreignPlan = runningPlan();
        foreignPlan.setPlanId(99L);
        foreignPlan.setTopicId(2L);
        FundProjectBudget foreignBudget = new FundProjectBudget();
        foreignBudget.setBudgetId(2L);
        foreignBudget.setTopicId(2L);
        foreignBudget.setTotalAmount(money("100"));
        when(budgetMapper.selectByTopicIdForUpdate(2L)).thenReturn(foreignBudget);
        when(planMapper.selectById(99L)).thenReturn(foreignPlan);
        when(planMapper.selectForUpdate(99L)).thenReturn(foreignPlan);
        doThrow(new ServiceException("无课题访问权限"))
                .when(permissionService).assertGroupMember(2L, 10L);
        doThrow(new ServiceException("无计划操作权限"))
                .when(permissionService).assertCanOperateAllocation(foreignPlan, 10L);
        login(10L);
        assertDenied(() -> service.selectPlan(99L));

        FundAllocationRecord stored = new FundAllocationRecord();
        stored.setRecordId(88L);
        stored.setPlanId(99L);
        stored.setSubmitUserId(10L);
        when(recordMapper.selectById(88L)).thenReturn(stored);
        FundAllocationRecord update = new FundAllocationRecord();
        update.setRecordId(88L);
        update.setAmount(money("10"));
        assertDenied(() -> service.updateRecord(update));
        verify(recordMapper, never()).update(any(FundAllocationRecord.class));
    }

    private void assertFinish(String actual, String type, boolean confirm)
    {
        when(recordMapper.sumByPlanId(10L)).thenReturn(money(actual));
        FundFinishCheckVo result = service.finishCheck(10L);
        assertEquals(type, result.getFinishType());
        assertEquals(confirm, result.isNeedConfirm());
    }

    private FundFinishRequest confirmed(String reason)
    {
        FundFinishRequest request = new FundFinishRequest();
        request.setConfirmDifference(true);
        request.setReason(reason);
        return request;
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

    private FundAllocationRecord allocationRecord(Long recordId, String amount)
    {
        FundAllocationRecord record = new FundAllocationRecord();
        record.setRecordId(recordId);
        record.setPlanId(10L);
        record.setAmount(money(amount));
        record.setSubmitUserId(1L);
        return record;
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
