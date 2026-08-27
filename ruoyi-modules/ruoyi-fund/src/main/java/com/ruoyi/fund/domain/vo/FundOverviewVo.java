package com.ruoyi.fund.domain.vo;
import java.math.BigDecimal;
public class FundOverviewVo {
    private Long topicId; private String topicName;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal plannedAllocationAmount = BigDecimal.ZERO;
    private BigDecimal arrivedAmount = BigDecimal.ZERO;
    private BigDecimal usedAmount = BigDecimal.ZERO;
    private BigDecimal pendingAllocationAmount = BigDecimal.ZERO;
    private BigDecimal remainingUseAmount = BigDecimal.ZERO;
    public Long getTopicId(){return topicId;} public void setTopicId(Long v){topicId=v;}
    public String getTopicName(){return topicName;} public void setTopicName(String v){topicName=v;}
    public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;}
    public BigDecimal getPlannedAllocationAmount(){return plannedAllocationAmount;} public void setPlannedAllocationAmount(BigDecimal v){plannedAllocationAmount=v;}
    public BigDecimal getArrivedAmount(){return arrivedAmount;} public void setArrivedAmount(BigDecimal v){arrivedAmount=v;}
    public BigDecimal getUsedAmount(){return usedAmount;} public void setUsedAmount(BigDecimal v){usedAmount=v;}
    public BigDecimal getPendingAllocationAmount(){return pendingAllocationAmount;} public void setPendingAllocationAmount(BigDecimal v){pendingAllocationAmount=v;}
    public BigDecimal getRemainingUseAmount(){return remainingUseAmount;} public void setRemainingUseAmount(BigDecimal v){remainingUseAmount=v;}
}
