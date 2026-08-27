package com.ruoyi.fund.mapper;
import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.fund.domain.FundUseRecord;
public interface FundUseRecordMapper {
    FundUseRecord selectById(Long useRecordId);
    List<FundUseRecord> selectByPlanId(Long usePlanId);
    BigDecimal sumByPlanId(Long usePlanId);
    BigDecimal sumByTopicId(Long topicId);
    int countByPlanId(Long usePlanId);
    int insert(FundUseRecord record);
    int update(FundUseRecord record);
    int deleteById(Long useRecordId);
}
