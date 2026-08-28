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
import com.ruoyi.fund.mapper.FundAllocationRecordMapper;
import com.ruoyi.fund.mapper.FundUsePlanMapper;
import com.ruoyi.fund.mapper.FundUseRecordMapper;
import com.ruoyi.fund.service.FundPermissionService;
import com.ruoyi.fund.service.IFundAttachmentService;
import com.ruoyi.fund.service.IFundOperationLogService;
import com.ruoyi.fund.service.IFundOrgService;
import com.ruoyi.fund.service.IFundResearchService;
import com.ruoyi.system.api.domain.FundUserOption;

@RunWith(MockitoJUnitRunner.class)
public class FundUseServiceImplTest
{
    @InjectMocks private FundUseServiceImpl service;
    @Mock private FundUsePlanMapper planMapper;
    @Mock private FundUseRecordMapper recordMapper;
    @Mock private FundProjectBudgetMapper budgetMapper;
    @Mock private FundAllocationRecordMapper allocationRecordMapper;
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
        when(researchService.isGroupMember(1L, 1L)).thenReturn(true);
        FundUserOption responsible = new FundUserOption();
        responsible.setUserName("user1");
        responsible.setNickName("User One");
        when(org.getUser(1L)).thenReturn(responsible);
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
    public void insertUseWithinActualAllocationSucceeds()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("100"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("60"));
        when(recordMapper.insert(any(FundUseRecord.class))).thenAnswer(invocation -> {
            ((FundUseRecord) invocation.getArgument(0)).setUseRecordId(20L);
            return 1;
        });

        assertEquals(1, service.insertRecord(useRecord(null, "40")));
    }

    @Test
    public void insertUseExceedingActualAllocationFailsBeforeInsert()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("100"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("90"));

        assertDenied(() -> service.insertRecord(useRecord(null, "20")));

        verify(recordMapper, never()).insert(any(FundUseRecord.class));
        verify(attachmentService, never()).consume(anyLong(), anyString(), anyLong(), anyString());
        verify(audit, never()).record(anyLong(), anyString(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    public void updateUseUsesDeltaCalculation()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.selectById(20L)).thenReturn(useRecord(20L, "50"));
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("90"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("95"));
        when(recordMapper.update(any(FundUseRecord.class))).thenReturn(1);

        assertEquals(1, service.updateRecord(useRecord(20L, "40")));
    }

    @Test
    public void updateUseCannotExceedActualAllocation()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.selectById(20L)).thenReturn(useRecord(20L, "10"));
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("90"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("80"));

        assertDenied(() -> service.updateRecord(useRecord(20L, "30")));
        verify(recordMapper, never()).update(any(FundUseRecord.class));
    }

    @Test
    public void deleteUseRecordStillMaintainsValidTotals()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(runningPlan());
        when(recordMapper.selectById(20L)).thenReturn(useRecord(20L, "20"));
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("20"));
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("100"));
        when(recordMapper.deleteById(20L)).thenReturn(1);

        assertEquals(1, service.deleteRecord(20L));
        verify(attachmentService).deleteByBusiness("USE_RECORD", 20L);
    }

    @Test
    public void pendingForceFinishRejectsNewUseRecord()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(pendingForceFinishPlan());
        assertDenied(() -> service.insertRecord(useRecord(null, "1")));
        verify(recordMapper, never()).insert(any(FundUseRecord.class));
    }

    @Test
    public void pendingForceFinishRejectsUseRecordUpdate()
    {
        when(recordMapper.selectById(20L)).thenReturn(useRecord(20L, "1"));
        when(planMapper.selectForUpdate(10L)).thenReturn(pendingForceFinishPlan());
        assertDenied(() -> service.updateRecord(useRecord(20L, "2")));
        verify(recordMapper, never()).update(any(FundUseRecord.class));
    }

    @Test
    public void pendingForceFinishRejectsUseRecordDelete()
    {
        when(recordMapper.selectById(20L)).thenReturn(useRecord(20L, "1"));
        when(planMapper.selectForUpdate(10L)).thenReturn(pendingForceFinishPlan());
        assertDenied(() -> service.deleteRecord(20L));
        verify(recordMapper, never()).deleteById(anyLong());
    }

    @Test
    public void pendingForceFinishRejectsRepeatedFinishRequest()
    {
        when(planMapper.selectForUpdate(10L)).thenReturn(pendingForceFinishPlan());
        assertDenied(() -> service.finish(10L, confirmed("repeat")));
        verify(planMapper, never()).requestForceFinish(anyLong(), any(), any(), anyString(), anyLong(), anyString());
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
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("0"), money("100"));
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("100"));

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
    public void finishPersistsUnderExactAndLeaderConfirmedOverExecutionResults()
    {
        FundProjectBudget budget = new FundProjectBudget();
        budget.setBudgetId(1L);
        budget.setTopicId(1L);
        budget.setTotalAmount(money("200"));
        when(budgetMapper.selectByTopicIdForUpdate(1L)).thenReturn(budget);
        FundUsePlan under = runningPlan();
        FundUsePlan exact = runningPlan();
        exact.setUsePlanId(11L);
        FundUsePlan over = runningPlan();
        over.setUsePlanId(12L);
        when(planMapper.selectForUpdate(10L)).thenReturn(under);
        when(planMapper.selectForUpdate(11L)).thenReturn(exact);
        when(planMapper.selectForUpdate(12L)).thenReturn(over);
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("120"));
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("200"));
        when(recordMapper.sumByPlanId(10L)).thenReturn(money("80"));
        when(recordMapper.sumByPlanId(11L)).thenReturn(money("100"));
        when(recordMapper.sumByPlanId(12L)).thenReturn(money("120"));
        when(permissionService.canConfirmForceFinish(1L, 1L)).thenReturn(true);

        service.finish(10L, confirmed("剩余计划不再使用"));
        service.finish(11L, new FundFinishRequest());
        service.finish(12L, confirmed("负责人确认超计划使用"));

        verify(planMapper).finish(10L, money("80"), money("-20"), FundConstants.FINISH_UNDER,
                "剩余计划不再使用", false, 1L, null, "user1");
        verify(planMapper).finish(11L, money("100"), money("0"), FundConstants.FINISH_NORMAL,
                null, false, 1L, null, "user1");
        verify(planMapper).finish(12L, money("120"), money("20"), FundConstants.FINISH_OVER,
                "负责人确认超计划使用", true, 1L, 1L, "user1");
    }

    @Test
    public void nonLeaderOveruseRequiresLeaderConfirmationBeforeCompletion()
    {
        FundProjectBudget budget = new FundProjectBudget();
        budget.setBudgetId(1L);
        budget.setTopicId(1L);
        budget.setTotalAmount(money("200"));
        when(budgetMapper.selectByTopicIdForUpdate(1L)).thenReturn(budget);
        FundUsePlan plan = runningPlan();
        when(planMapper.selectForUpdate(10L)).thenReturn(plan);
        when(recordMapper.sumByTopicId(1L)).thenReturn(money("120"));
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("200"));
        when(recordMapper.sumByPlanId(10L)).thenReturn(money("120"));
        when(permissionService.canConfirmForceFinish(1L, 1L)).thenReturn(false);

        service.finish(10L, confirmed("申请负责人确认超计划使用"));
        verify(planMapper).requestForceFinish(10L, money("120"), money("20"),
                "申请负责人确认超计划使用", 1L, "user1");

        plan.setForceFinish("1");
        plan.setFinishType(FundConstants.FINISH_OVER);
        plan.setFinishReason("申请负责人确认超计划使用");
        plan.setFinishUserId(1L);
        service.confirmForceFinish(10L);
        verify(permissionService).assertGroupLeader(1L, 1L);
        verify(planMapper).confirmForceFinish(10L, money("120"), money("20"), 1L, "user1");
    }

    @Test
    public void useRecordInsertWaitingBehindSuccessfulCloseIsRejected() throws Exception
    {
        FundUsePlan plan = runningPlan();
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
        when(allocationRecordMapper.sumByTopicId(1L)).thenReturn(money("100"));
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
        }, "use-close");
        Thread recordThread = new Thread(() -> {
            try
            {
                FundUseRecord record = new FundUseRecord();
                record.setUsePlanId(10L);
                record.setAmount(money("1"));
                service.insertRecord(record);
            }
            catch (Throwable error)
            {
                recordFailure.set(error);
            }
        }, "use-record-after-close");

        closeThread.start();
        assertTrue(closeHasLock.await(5, TimeUnit.SECONDS));
        recordThread.start();
        closeThread.join(5000);
        recordThread.join(5000);

        assertFalse(closeThread.isAlive());
        assertFalse(recordThread.isAlive());
        assertNull(closeFailure.get());
        assertTrue(recordFailure.get() instanceof ServiceException);
        verify(recordMapper, never()).insert(any(FundUseRecord.class));
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
        doThrow(new ServiceException("无计划操作权限"))
                .when(permissionService).assertCanOperateUse(foreignPlan, 10L);
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

    private FundFinishRequest confirmed(String reason)
    {
        FundFinishRequest request = new FundFinishRequest();
        request.setConfirmDifference(true);
        request.setReason(reason);
        return request;
    }

    private FundUsePlan plan(BigDecimal amount)
    {
        FundUsePlan plan = runningPlan();
        plan.setUsePlanId(null);
        plan.setPlanAmount(amount);
        plan.setResponsibleUserId(1L);
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

    private FundUsePlan pendingForceFinishPlan()
    {
        FundUsePlan plan = runningPlan();
        plan.setForceFinish("1");
        plan.setFinishType(FundConstants.FINISH_OVER);
        plan.setFinishUserId(1L);
        plan.setFinishReason("waiting for leader confirmation");
        return plan;
    }

    private FundUseRecord useRecord(Long recordId, String amount)
    {
        FundUseRecord record = new FundUseRecord();
        record.setUseRecordId(recordId);
        record.setUsePlanId(10L);
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
