package com.ruoyi.fund.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import org.junit.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.fund.domain.FundAllocationPlan;
import com.ruoyi.fund.domain.FundAllocationRecord;
import com.ruoyi.fund.domain.FundProjectBudget;
import com.ruoyi.fund.domain.FundUsePlan;
import com.ruoyi.fund.domain.FundUseRecord;
import com.ruoyi.fund.domain.dto.FundFinishRequest;

public class FundTransactionIsolationTest
{
    @Test
    public void projectAmountWritesUseReadCommittedAfterBudgetLock() throws Exception
    {
        assertReadCommitted(FundBudgetServiceImpl.class, "update", FundProjectBudget.class);

        assertReadCommitted(FundAllocationServiceImpl.class, "insertPlan", FundAllocationPlan.class);
        assertReadCommitted(FundAllocationServiceImpl.class, "updatePlan", FundAllocationPlan.class);
        assertReadCommitted(FundAllocationServiceImpl.class, "deletePlan", Long.class);
        assertReadCommitted(FundAllocationServiceImpl.class, "insertRecord", FundAllocationRecord.class);
        assertReadCommitted(FundAllocationServiceImpl.class, "updateRecord", FundAllocationRecord.class);
        assertReadCommitted(FundAllocationServiceImpl.class, "deleteRecord", Long.class);
        assertReadCommitted(FundAllocationServiceImpl.class, "finish", Long.class, FundFinishRequest.class);

        assertReadCommitted(FundUseServiceImpl.class, "insertPlan", FundUsePlan.class);
        assertReadCommitted(FundUseServiceImpl.class, "updatePlan", FundUsePlan.class);
        assertReadCommitted(FundUseServiceImpl.class, "deletePlan", Long.class);
        assertReadCommitted(FundUseServiceImpl.class, "insertRecord", FundUseRecord.class);
        assertReadCommitted(FundUseServiceImpl.class, "updateRecord", FundUseRecord.class);
        assertReadCommitted(FundUseServiceImpl.class, "deleteRecord", Long.class);
        assertReadCommitted(FundUseServiceImpl.class, "finish", Long.class, FundFinishRequest.class);
        assertReadCommitted(FundUseServiceImpl.class, "confirmForceFinish", Long.class);
    }

    private void assertReadCommitted(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws Exception
    {
        Method method = type.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(type.getSimpleName() + "." + methodName + " must be transactional", transactional);
        assertEquals(type.getSimpleName() + "." + methodName + " must see commits made while waiting for the budget lock",
                Isolation.READ_COMMITTED, transactional.isolation());
    }
}
