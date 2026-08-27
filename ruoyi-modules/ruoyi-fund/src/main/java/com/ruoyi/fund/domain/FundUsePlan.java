package com.ruoyi.fund.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FundUsePlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 使用计划ID */
    private Long usePlanId;
    /** 预算ID */
    private Long budgetId;
    /** 课题ID */
    @NotNull(message = "课题不能为空")
    private Long topicId;
    /** 课题名称 */
    private String topicName;
    /** 使用计划名称 */
    @NotBlank(message = "使用名称不能为空")
    private String useName;
    /** 使用计划金额 */
    @NotNull(message = "使用金额不能为空")
    @DecimalMin(value = "0.01", message = "使用金额必须大于0")
    private BigDecimal planAmount;
    /** 责任人ID */
    private Long responsibleUserId;
    /** 责任人名称快照 */
    private String responsibleUserName;
    /** 计划时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planTime;
    /** 资金说明 */
    private String fundDesc;
    /** 状态 */
    private String status;
    /** 实际使用金额 */
    private BigDecimal actualAmount;
    /** 实际-计划差额 */
    private BigDecimal differenceAmount;
    /** 结束类型 */
    private String finishType;
    /** 结束人 */
    private Long finishUserId;
    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    public Long getUsePlanId() { return usePlanId; }
    public void setUsePlanId(Long usePlanId) { this.usePlanId = usePlanId; }
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getUseName() { return useName; }
    public void setUseName(String useName) { this.useName = useName; }
    public BigDecimal getPlanAmount() { return planAmount; }
    public void setPlanAmount(BigDecimal planAmount) { this.planAmount = planAmount; }
    public Long getResponsibleUserId() { return responsibleUserId; }
    public void setResponsibleUserId(Long responsibleUserId) { this.responsibleUserId = responsibleUserId; }
    public String getResponsibleUserName() { return responsibleUserName; }
    public void setResponsibleUserName(String responsibleUserName) { this.responsibleUserName = responsibleUserName; }
    public Date getPlanTime() { return planTime; }
    public void setPlanTime(Date planTime) { this.planTime = planTime; }
    public String getFundDesc() { return fundDesc; }
    public void setFundDesc(String fundDesc) { this.fundDesc = fundDesc; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(BigDecimal differenceAmount) { this.differenceAmount = differenceAmount; }
    public String getFinishType() { return finishType; }
    public void setFinishType(String finishType) { this.finishType = finishType; }
    public Long getFinishUserId() { return finishUserId; }
    public void setFinishUserId(Long finishUserId) { this.finishUserId = finishUserId; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
}
