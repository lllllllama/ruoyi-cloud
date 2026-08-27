package com.ruoyi.fund.mapper;
import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.fund.domain.FundAllocationRecord;
public interface FundAllocationRecordMapper {
    FundAllocationRecord selectById(Long recordId);
    List<FundAllocationRecord> selectByPlanId(Long planId);
    BigDecimal sumByPlanId(Long planId);
    BigDecimal sumByTopicId(Long topicId);
    int countByPlanId(Long planId);
    int insert(FundAllocationRecord record);
    int update(FundAllocationRecord record);
    int deleteById(Long recordId);
}
