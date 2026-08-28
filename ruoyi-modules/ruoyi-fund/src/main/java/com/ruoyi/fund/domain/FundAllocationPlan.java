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

public class FundAllocationPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 拨付计划ID */
    private Long planId;
    /** 预算ID */
    private Long budgetId;
    /** 课题ID */
    @NotNull(message = "课题不能为空")
    private Long topicId;
    /** 课题名称 */
    private String topicName;
    /** 拨付名称 */
    @NotBlank(message = "拨付名称不能为空")
    private String allocationName;
    /** 拨付单位ID */
    @NotNull(message = "拨付单位不能为空")
    private Long allocationDeptId;
    /** 拨付单位名称快照 */
    private String allocationDeptName;
    /** 接收单位ID */
    @NotNull(message = "接收单位不能为空")
    private Long receiveDeptId;
    /** 接收单位名称快照 */
    private String receiveDeptName;
    /** 计划拨付金额 */
    @NotNull(message = "拨付金额不能为空")
    @DecimalMin(value = "0.01", message = "拨付金额必须大于0")
    @Digits(integer = 16, fraction = 2, message = "拨付金额最多保留2位小数")
    private BigDecimal planAmount;
    /** 计划拨付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planTime;
    /** 资金说明 */
    private String fundDesc;
    /** 责任人ID */
    private Long responsibleUserId;
    /** 责任人名称快照 */
    private String responsibleUserName;
    /** 状态 */
    private String status;
    /** 实际拨付金额 */
    private BigDecimal actualAmount;
    /** 实际-计划差额 */
    private BigDecimal differenceAmount;
    /** 结束类型 */
    private String finishType;
    private String finishReason;
    /** 结束人 */
    private Long finishUserId;
    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
    /** Current-user capabilities; informational only, server checks remain authoritative. */
    private Boolean canSubmitRecord;
    private Boolean canAssignResponsible;
    private Boolean canFinish;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getAllocationName() { return allocationName; }
    public void setAllocationName(String allocationName) { this.allocationName = allocationName; }
    public Long getAllocationDeptId() { return allocationDeptId; }
    public void setAllocationDeptId(Long allocationDeptId) { this.allocationDeptId = allocationDeptId; }
    public String getAllocationDeptName() { return allocationDeptName; }
    public void setAllocationDeptName(String allocationDeptName) { this.allocationDeptName = allocationDeptName; }
    public Long getReceiveDeptId() { return receiveDeptId; }
    public void setReceiveDeptId(Long receiveDeptId) { this.receiveDeptId = receiveDeptId; }
    public String getReceiveDeptName() { return receiveDeptName; }
    public void setReceiveDeptName(String receiveDeptName) { this.receiveDeptName = receiveDeptName; }
    public BigDecimal getPlanAmount() { return planAmount; }
    public void setPlanAmount(BigDecimal planAmount) { this.planAmount = planAmount; }
    public Date getPlanTime() { return planTime; }
    public void setPlanTime(Date planTime) { this.planTime = planTime; }
    public String getFundDesc() { return fundDesc; }
    public void setFundDesc(String fundDesc) { this.fundDesc = fundDesc; }
    public Long getResponsibleUserId() { return responsibleUserId; }
    public void setResponsibleUserId(Long responsibleUserId) { this.responsibleUserId = responsibleUserId; }
    public String getResponsibleUserName() { return responsibleUserName; }
    public void setResponsibleUserName(String responsibleUserName) { this.responsibleUserName = responsibleUserName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(BigDecimal differenceAmount) { this.differenceAmount = differenceAmount; }
    public String getFinishType() { return finishType; }
    public void setFinishType(String finishType) { this.finishType = finishType; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    public Long getFinishUserId() { return finishUserId; }
    public void setFinishUserId(Long finishUserId) { this.finishUserId = finishUserId; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
    public Boolean getCanSubmitRecord() { return canSubmitRecord; }
    public void setCanSubmitRecord(Boolean canSubmitRecord) { this.canSubmitRecord = canSubmitRecord; }
    public Boolean getCanAssignResponsible() { return canAssignResponsible; }
    public void setCanAssignResponsible(Boolean canAssignResponsible) { this.canAssignResponsible = canAssignResponsible; }
    public Boolean getCanFinish() { return canFinish; }
    public void setCanFinish(Boolean canFinish) { this.canFinish = canFinish; }
}
