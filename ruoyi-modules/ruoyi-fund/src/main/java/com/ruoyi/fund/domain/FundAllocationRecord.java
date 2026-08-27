package com.ruoyi.fund.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FundAllocationRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 拨付记录ID */
    private Long recordId;
    /** 拨付计划ID */
    @NotNull(message = "拨付计划不能为空")
    private Long planId;
    /** 拨付记录名称 */
    @NotBlank(message = "拨付记录名称不能为空")
    private String allocationName;
    /** 实际拨付金额 */
    @NotNull(message = "拨付金额不能为空")
    @DecimalMin(value = "0.01", message = "拨付金额必须大于0")
    private BigDecimal amount;
    /** 拨付时间 */
    @NotNull(message = "拨付时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date allocationTime;
    /** 资金说明 */
    private String fundDesc;
    /** 拨付凭证URL，逗号分隔 */
    private String voucherUrls;
    /** 提交人ID */
    private Long submitUserId;
    /** 提交人名称 */
    private String submitUserName;
    private List<FundAttachment> attachments;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getAllocationName() { return allocationName; }
    public void setAllocationName(String allocationName) { this.allocationName = allocationName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Date getAllocationTime() { return allocationTime; }
    public void setAllocationTime(Date allocationTime) { this.allocationTime = allocationTime; }
    public String getFundDesc() { return fundDesc; }
    public void setFundDesc(String fundDesc) { this.fundDesc = fundDesc; }
    public String getVoucherUrls() { return voucherUrls; }
    public void setVoucherUrls(String voucherUrls) { this.voucherUrls = voucherUrls; }
    public Long getSubmitUserId() { return submitUserId; }
    public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }
    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }
    public List<FundAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<FundAttachment> attachments) { this.attachments = attachments; }
}
