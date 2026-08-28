package com.ruoyi.fund.domain.vo;

import java.math.BigDecimal;

public class FundAllocationOverviewVo
{
    private Long groupId;
    private BigDecimal totalAmount;
    private BigDecimal plannedAllocation;
    private BigDecimal actualAllocation;
    private BigDecimal remainingAllocation;
    private BigDecimal overAllocation;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPlannedAllocation() { return plannedAllocation; }
    public void setPlannedAllocation(BigDecimal plannedAllocation) { this.plannedAllocation = plannedAllocation; }
    public BigDecimal getActualAllocation() { return actualAllocation; }
    public void setActualAllocation(BigDecimal actualAllocation) { this.actualAllocation = actualAllocation; }
    public BigDecimal getRemainingAllocation() { return remainingAllocation; }
    public void setRemainingAllocation(BigDecimal remainingAllocation) { this.remainingAllocation = remainingAllocation; }
    public BigDecimal getOverAllocation() { return overAllocation; }
    public void setOverAllocation(BigDecimal overAllocation) { this.overAllocation = overAllocation; }
}
