package com.ruoyi.fund.domain.dto;
public class FundFinishRequest {
    private Boolean confirmDifference;
    private String reason;
    public Boolean getConfirmDifference() { return confirmDifference; }
    public void setConfirmDifference(Boolean confirmDifference) { this.confirmDifference = confirmDifference; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
