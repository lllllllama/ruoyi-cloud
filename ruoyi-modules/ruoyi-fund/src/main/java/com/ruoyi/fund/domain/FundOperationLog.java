package com.ruoyi.fund.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class FundOperationLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long groupId;
    private String businessType;
    private Long businessId;
    private String operationType;
    @JsonIgnore
    private String beforeData;
    @JsonIgnore
    private String afterData;
    private String reason;
    private Long operatorId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operationTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }
    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Date getOperationTime() { return operationTime; }
    public void setOperationTime(Date operationTime) { this.operationTime = operationTime; }
}
