package com.ruoyi.fund.domain.vo;

import java.math.BigDecimal;

public class FundUseOverviewVo
{
    private Long groupId;
    private BigDecimal plannedUse;
    private BigDecimal actualUse;
    private BigDecimal remainingUse;
    private BigDecimal overspend;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public BigDecimal getPlannedUse() { return plannedUse; }
    public void setPlannedUse(BigDecimal plannedUse) { this.plannedUse = plannedUse; }
    public BigDecimal getActualUse() { return actualUse; }
    public void setActualUse(BigDecimal actualUse) { this.actualUse = actualUse; }
    public BigDecimal getRemainingUse() { return remainingUse; }
    public void setRemainingUse(BigDecimal remainingUse) { this.remainingUse = remainingUse; }
    public BigDecimal getOverspend() { return overspend; }
    public void setOverspend(BigDecimal overspend) { this.overspend = overspend; }
}
