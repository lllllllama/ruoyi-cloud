package com.ruoyi.fund.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FundProjectBudget extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 预算ID */
    private Long budgetId;
    /** 课题ID */
    @NotNull(message = "课题不能为空")
    private Long topicId;
    /** 课题名称 */
    private String topicName;
    /** 课题总资金 */
    @NotNull(message = "课题总资金不能为空")
    @DecimalMin(value = "0.01", message = "课题总资金必须大于0")
    @Digits(integer = 16, fraction = 2, message = "课题总资金最多保留2位小数")
    private BigDecimal totalAmount;
    /** 计划拨付完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planEndTime;
    /** 资金说明 */
    private String fundDesc;
    /** 状态 */
    private String status;

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Date getPlanEndTime() { return planEndTime; }
    public void setPlanEndTime(Date planEndTime) { this.planEndTime = planEndTime; }
    public String getFundDesc() { return fundDesc; }
    public void setFundDesc(String fundDesc) { this.fundDesc = fundDesc; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
