package com.ruoyi.fund.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FundUseRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 使用记录ID */
    private Long useRecordId;
    /** 使用计划ID */
    @NotNull(message = "使用计划不能为空")
    private Long usePlanId;
    /** 使用记录名称 */
    @NotBlank(message = "使用记录名称不能为空")
    private String useName;
    /** 实际使用金额 */
    @NotNull(message = "使用金额不能为空")
    @DecimalMin(value = "0.01", message = "使用金额必须大于0")
    private BigDecimal amount;
    /** 使用时间 */
    @NotNull(message = "使用时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useTime;
    /** 资金说明 */
    private String fundDesc;
    /** 使用凭证URL，逗号分隔 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String voucherUrls;
    /** 提交人ID */
    private Long submitUserId;
    /** 提交人名称 */
    private String submitUserName;
    private List<FundAttachment> attachments;

    public Long getUseRecordId() { return useRecordId; }
    public void setUseRecordId(Long useRecordId) { this.useRecordId = useRecordId; }
    public Long getUsePlanId() { return usePlanId; }
    public void setUsePlanId(Long usePlanId) { this.usePlanId = usePlanId; }
    public String getUseName() { return useName; }
    public void setUseName(String useName) { this.useName = useName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Date getUseTime() { return useTime; }
    public void setUseTime(Date useTime) { this.useTime = useTime; }
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
